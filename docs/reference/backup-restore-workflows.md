# Mô hình và quy trình Backup / Restore

**Trạng thái:** tài liệu triển khai, cập nhật 2026-11-06  
**Phạm vi:** FR015 (sao lưu), FR016 (phục hồi), HealthMonitor và cơ chế tự động phục hồi.  
**Nguồn tham chiếu:** mã nguồn trong `src/main/java/com/dtaq/retail/` và các quyết định implementation đã ghi tại [`README.md`](README.md) §5.1. Tài liệu này **không thay thế** DOCX gốc.

---

## 1. Mô hình tổng thể

```mermaid
flowchart TD
    subgraph UI["Giao diện Quản lý"]
        backupPage["BackupPage<br/>Sao lưu dữ liệu"]
        restorePage["RestorePage<br/>Phục hồi dữ liệu"]
        incidentDialog["IncidentDialog<br/>Sự cố dữ liệu"]
    end

    subgraph Controller["Controller"]
        backupController["BackupController"]
        incidentController["IncidentController"]
    end

    subgraph Service["Service"]
        backupService["BackupService"]
        restoreService["RestoreService"]
        backupScheduler["BackupScheduler"]
        healthMonitor["HealthMonitor"]
        recoveryCoordinator["RecoveryCoordinator"]
        dataHealthChecker["DataHealthChecker"]
    end

    subgraph Infra["Hạ tầng"]
        pgCommandExecutor["PgCommandExecutor"]
        backupStore["BackupStore"]
        maintenanceLock["MaintenanceLock"]
        appState["AppState"]
        connectDB["ConnectDB<br/>SHARED DataSource"]
    end

    subgraph Storage["Lưu trữ"]
        backupFiles["*.dump + *.meta.json"]
        healthState["health_state.json"]
        historyTable["LichSuSaoLuu"]
        incidentTable["SuCoSucKhoe"]
        postgres["PostgreSQL"]
    end

    backupPage --> backupController
    restorePage --> backupController
    incidentDialog --> incidentController
    backupController --> backupService
    backupController --> restoreService
    incidentController --> restoreService
    backupScheduler --> backupService
    healthMonitor --> dataHealthChecker
    healthMonitor --> recoveryCoordinator
    recoveryCoordinator --> restoreService
    recoveryCoordinator --> backupService
    recoveryCoordinator --> appState
    backupService --> maintenanceLock
    restoreService --> maintenanceLock
    backupService --> pgCommandExecutor
    restoreService --> pgCommandExecutor
    backupService --> backupStore
    restoreService --> backupStore
    backupStore --> backupFiles
    recoveryCoordinator --> healthState
    backupService --> historyTable
    restoreService --> historyTable
    recoveryCoordinator --> incidentTable
    pgCommandExecutor --> postgres
    connectDB --> postgres
```

### Nguyên tắc thiết kế

- **Nguồn sự thật** của một bản sao lưu là cặp tệp `*.dump` + `*.meta.json` trong thư mục backup.
- Bảng `"LichSuSaoLuu"` và `"SuCoSucKhoe"` chỉ là **cache hiển thị**; có thể dựng lại bằng cách quét tệp.
- `health_state.json` lưu baseline, streak và số lần auto restore **ngoài DB**, vì DB có thể là thứ bị mất.
- `MaintenanceLock` bảo đảm chỉ một thao tác nặng (backup, restore, health check) tại một thời điểm.
- `ConnectDB` trả về `SHARED` proxy: sau khi restore reset pool, các DAO cũ vẫn lấy được pool mới.

---

## 2. Mô hình dữ liệu

```mermaid
flowchart LR
    subgraph files["Filesystem - nguồn sự thật"]
        dump["dtaq_yyyy-MM-dd_HHmmss.dump"]
        meta["dtaq_...meta.json<br/>kích thước, SHA-256, loại, trạng thái"]
        healthState["health_state.json<br/>baseline, streak, attempts"]
    end

    subgraph db["PostgreSQL - cache hiển thị"]
        history["LichSuSaoLuu<br/>filePath, loại, trạng thái"]
        incident["SuCoSucKhoe<br/>severity, trạng thái, backupUsed"]
        business["14 bảng nghiệp vụ<br/>NhanVien, HoaDon, KhachHang..."]
    end

    dump --> meta
    dump --> history
    meta --> history
    healthState --> incident
    history --> incident
    business --> incident
```

| Thành phần | Vai trò | Ghi chú |
|---|---|---|
| `*.dump` | Bản dump toàn bộ DB (`pg_dump -Fc`) | Nguồn sự thật |
| `*.meta.json` | Kích thước, SHA-256, loại, trạng thái, phiên bản PG, tên CSDL | Nguồn sự thật |
| `"LichSuSaoLuu"` | Lịch sử backup/restore để hiển thị | Cache |
| `"SuCoSucKhoe"` | Incident `SUSPICIOUS`/`CRITICAL` | Cache |
| `health_state.json` | Baseline, streak, `autoRestoreAttempts`, `openIncidentId` | Ngoài DB |

---

## 3. Backup thủ công

**Trigger:** Quản lý bấm **“Sao lưu ngay”** trên trang `Sao lưu dữ liệu`.

```mermaid
sequenceDiagram
    actor QL as Quản lý
    participant UI as BackupPage
    participant C as BackupController
    participant S as BackupService
    participant L as MaintenanceLock
    participant PG as PgCommandExecutor
    participant DB as PostgreSQL
    participant Store as BackupStore
    participant H as LichSuSaoLuu

    QL->>UI: Bấm Sao lưu ngay
    UI->>C: saoLuuNgay()
    C->>C: Kiểm tra quyền NHAN_VIEN_QUAN_LY
    C->>S: saoLuu(MANUAL, maNV)
    S->>L: tryAcquire(5 giây)
    alt Không lấy được khóa
        S-->>C: BackupException - hệ thống đang bận
        C-->>UI: Lỗi
        UI-->>QL: Toast + thông báo
    else Có khóa
        S->>PG: pg_dump -Fc ... ra .dump.tmp
        PG->>DB: Đọc toàn bộ DB
        DB-->>PG: Dữ liệu
        PG-->>S: Exit code + output
        S->>S: Kiểm tra size > 0
        S->>PG: pg_restore --list .dump.tmp
        S->>S: Tính SHA-256
        S->>Store: Đổi tên .tmp thành .dump
        S->>Store: Ghi .meta.json
        S->>H: Insert lịch sử SUCCESS
        S->>Store: Copy thư mục phụ + retention
        S-->>C: BackupRecord
        C-->>UI: Thành công
        UI-->>QL: Toast + tray notification
        S->>L: release()
    end
    Note over S,H: Nếu dump lỗi: xóa .tmp, ghi meta/history FAILED, không đổi tên
```

### Các bước cốt lõi

1. Lấy `MaintenanceLock` (timeout 5 giây).
2. `pg_dump -Fc --no-owner --no-privileges` ra file tạm `.dump.tmp`.
3. Kiểm tra exit code, kích thước > 0, `pg_restore --list` chạy được.
4. Tính SHA-256.
5. Đổi tên nguyên tử `.tmp` → `.dump`.
6. Ghi `.meta.json`.
7. Ghi lịch sử `"LichSuSaoLuu"` (best effort).
8. Copy sang thư mục phụ nếu có.
9. Retention: xóa bản cũ hơn 7 ngày, **không bao giờ xóa bản SUCCESS mới nhất**.
10. Nhả khóa.

---

## 4. Backup tự động

**Trigger:** `BackupScheduler` chạy lúc khởi động (chạy bù) và theo lịch `DTAQ_BACKUP_TIME` + chu kỳ `DTAQ_BACKUP_INTERVAL_HOURS`.

```mermaid
sequenceDiagram
    participant Main
    participant Sched as BackupScheduler
    participant State as AppState
    participant S as BackupService
    participant Lock as MaintenanceLock
    participant Store as BackupStore

    Main->>Sched: start()
    Sched->>Sched: Kiểm tra DTAQ_BACKUP_ENABLED
    Sched->>Store: Tìm bản SUCCESS gần nhất
    Sched->>Sched: canChayBu(lần cuối, now, chu kỳ)
    alt Quá hạn
        Sched->>State: isNormal()?
        alt Không NORMAL
            Sched-->>Sched: Bỏ qua, ghi log
        else NORMAL
            Sched->>S: saoLuu(AUTO, SYSTEM)
            S->>Lock: tryAcquire(5 giây)
            S->>S: Quy trình backup như thủ công
        end
    end
    Sched->>Sched: scheduleAtFixedRate(giờ chạy, chu kỳ giờ)
    Note over Sched: Khi Quản lý lưu giờ/chu kỳ mới:<br/>lapLichLai() hủy lịch cũ và lập lại
```

### Điều kiện và hành vi

| Điều kiện | Hành vi |
|---|---|
| `DTAQ_BACKUP_ENABLED=false` | Không tạo lịch |
| Chưa có bản SUCCESS hoặc bản gần nhất cũ hơn chu kỳ | Chạy bù ngay khi khởi động |
| `AppState != NORMAL` | Bỏ qua lần chạy, ghi log |
| Đang có lần chạy khác | Bỏ qua nhờ `AtomicBoolean dangChay` |
| Quản lý đổi giờ/chu kỳ trên UI | `lapLichLai()` hủy lịch cũ, lập lịch mới |

- Actor ghi lịch sử là `SYSTEM`, loại `AUTO`.
- Chu kỳ là **khoảng lặp thực tế**, neo theo giờ chạy: `24` = mỗi ngày, `6` = 4 lần/ngày.
- Không chạy backup tự động khi dữ liệu đang `SUSPICIOUS`, `UNAVAILABLE` hoặc đang `MAINTENANCE`.

---

## 5. Restore thủ công

**Trigger:** Quản lý chọn một bản backup trên trang `Phục hồi dữ liệu`.

```mermaid
sequenceDiagram
    actor QL as Quản lý
    participant UI as RestorePage
    participant D as RestoreConfirmDialog
    participant C as BackupController
    participant R as RestoreService
    participant B as BackupService
    participant L as MaintenanceLock
    participant PG as PgCommandExecutor
    participant Conn as ConnectDB
    participant DB as PostgreSQL

    QL->>UI: Chọn bản sao lưu
    UI->>UI: Kiểm tra tệp - SHA + pg_restore --list
    QL->>UI: Bấm Phục hồi
    UI->>D: Mở xác nhận 2 bước
    D->>D: Bước 1 - tóm tắt + cảnh báo
    D->>D: Bước 2 - tick hiểu mất dữ liệu
    D-->>UI: onConfirm
    UI->>C: phucHoi(record, true)
    C->>C: Kiểm tra quyền
    C->>R: phucHoi(record, maNV, true, MANUAL_RESTORE)
    R->>R: Bắt buộc daXacNhan
    R->>L: tryAcquire(5 giây)
    R->>B: kiemTra(record)
    R->>R: AppState = MAINTENANCE
    R->>B: saoLuu(SAFETY) - best effort
    R->>Conn: terminateOtherConnections()
    R->>Conn: close()
    R->>PG: pg_restore --clean --if-exists<br/>--no-owner --single-transaction
    PG->>DB: Ghi đè toàn bộ DB
    alt DB không tồn tại
        R->>PG: createdb
        R->>PG: pg_restore lần 2
    end
    R->>Conn: reset() trong finally
    R->>Conn: isAvailable()
    R->>R: Kiểm tra đủ 14 bảng
    R->>R: Ghi lịch sử MANUAL_RESTORE
    R->>R: Khôi phục AppState, release lock
    R-->>UI: BackupRecord
    UI-->>QL: Toast + tray notification
```

### Các bước cốt lõi

1. Bắt buộc `daXacNhan = true` (đã qua 2 bước).
2. Lấy `MaintenanceLock`.
3. `BackupService.kiemTra`: file tồn tại, đúng size, đúng SHA-256, `pg_restore --list` OK.
4. Đặt `AppState = MAINTENANCE`.
5. Tạo **safety backup** hiện trạng (`SAFETY`), lỗi chỉ cảnh báo.
6. Ngắt kết nối khác, đóng pool.
7. `pg_restore --clean --if-exists --no-owner --no-privileges --single-transaction`.
8. Nếu DB không tồn tại → `createdb` → thử lại một lần.
9. `ConnectDB.reset()` mở pool mới; kiểm tra kết nối.
10. Kiểm tra đủ 14 bảng cốt lõi.
11. Ghi lịch sử `MANUAL_RESTORE` SUCCESS/FAILED.
12. Khôi phục `AppState`, nhả khóa.

> `--single-transaction` bảo đảm restore **toàn bộ hoặc không gì cả**; lỗi giữa chừng sẽ rollback.

---

## 6. Restore tự động

**Trigger:** `HealthMonitor` phát hiện `CRITICAL`.

```mermaid
sequenceDiagram
    participant HM as HealthMonitor
    participant HC as DataHealthChecker
    participant RC as RecoveryCoordinator
    participant BS as BackupService
    participant RS as RestoreService
    participant DB as PostgreSQL

    HM->>HC: kiemTra()
    HC-->>HM: HealthReport CRITICAL
    HM->>RC: xuLy(report)
    RC->>BS: danhSach() + kiemTra()
    BS-->>RC: Danh sách backup hợp lệ
    RC->>RS: phucHoi(backup, SYSTEM, true, AUTO_RESTORE)
    RS->>DB: pg_restore --single-transaction
    DB-->>RS: Thành công
    RS-->>RC: BackupRecord AUTO_RESTORE
    RC->>RC: Incident AUTO_RESTORED<br/>AppState NORMAL
```

### Điều kiện để auto restore chạy

| Điều kiện | Giá trị |
|---|---|
| `DTAQ_HEALTH_ENABLED` | `true` |
| `DTAQ_HEALTH_AUTO_RESTORE_ENABLED` | `true` |
| Trạng thái | `CRITICAL` |
| Số lần trong 1 giờ | < `DTAQ_HEALTH_AUTO_RESTORE_MAX_PER_HOUR` (3) |
| Backup hợp lệ | Có ít nhất 1 bản SUCCESS, SHA-256 + `pg_restore --list` OK |
| Khóa bảo trì | Không bận |

### CRITICAL gồm 3 trường hợp

1. Database không tồn tại — SQLState `3Dxxx`.
2. Thiếu bảng cốt lõi.
3. Tổng số dòng của **tất cả** 14 bảng cốt lõi = 0 (schema rỗng).

### Không auto restore

- `SUSPICIOUS`: dữ liệu giảm ≥ 50%, baseline ≥ 50, đủ 2 chu kỳ → incident `PENDING`, chờ Quản lý.
- `UNAVAILABLE`: server tắt / sai mật khẩu / mạng → chỉ cảnh báo.
- Auto restore bị tắt, vượt 3 lần/giờ, không có backup hợp lệ, hoặc đang bảo trì.

---

## 7. Cây quyết định của HealthMonitor

```mermaid
flowchart TD
    A["HealthMonitor<br/>khởi động + mỗi 5 phút"] --> B{"DTAQ_HEALTH_ENABLED?"}
    B -->|"false"| Z["Bỏ qua"]
    B -->|"true"| C{"MaintenanceLock bận?"}
    C -->|"có"| Z
    C -->|"không"| D["DataHealthChecker.kiemTra()"]
    D --> E{"Kết nối DB?"}
    E -->|"SQLState 3D"| F["CRITICAL"]
    E -->|"08 / 28 / khác"| G["UNAVAILABLE"]
    E -->|"được"| H{"Thiếu bảng cốt lõi?"}
    H -->|"có"| F
    H -->|"không"| I{"Tổng 14 bảng = 0?"}
    I -->|"có"| F
    I -->|"không"| J{"Bảng giảm >= ngưỡng<br/>và baseline >= 50?"}
    J -->|"có"| K["SUSPICIOUS"]
    J -->|"không"| L["HEALTHY"]
    F --> M{"Auto restore bật?"}
    M -->|"không"| N["Incident PENDING<br/>báo Quản lý"]
    M -->|"có"| O{"Vượt 3 lần/giờ?"}
    O -->|"có"| P["Incident FAILED<br/>báo Quản lý"]
    O -->|"không"| Q{"Có backup hợp lệ?"}
    Q -->|"không"| P
    Q -->|"có"| R["Thử từng bản mới nhất<br/>tối đa 3 bản"]
    R --> S{"Restore thành công?"}
    S -->|"có"| T["Incident AUTO_RESTORED<br/>AppState NORMAL"]
    S -->|"không"| P
    K --> U{"Đủ 2 chu kỳ?"}
    U -->|"chưa"| V["Ghi streak<br/>chưa báo"]
    U -->|"đủ"| N
    G --> W["Chỉ cảnh báo<br/>AppState UNAVAILABLE"]
    L --> X["Cập nhật baseline<br/>đóng incident<br/>AppState NORMAL"]
```

---

## 8. Máy trạng thái `AppState`

```mermaid
stateDiagram-v2
    [*] --> NORMAL
    NORMAL --> MAINTENANCE: restore bắt đầu
    MAINTENANCE --> NORMAL: restore thành công
    MAINTENANCE --> SUSPICIOUS: restore thất bại
    NORMAL --> SUSPICIOUS: SUSPICIOUS đủ chu kỳ
    NORMAL --> UNAVAILABLE: không kết nối DB
    SUSPICIOUS --> NORMAL: Quản lý bỏ qua / phục hồi xong
    UNAVAILABLE --> NORMAL: DB hoạt động lại
    SUSPICIOUS --> MAINTENANCE: Quản lý xác nhận phục hồi
```

---

## 9. So sánh Manual và Automation

| Tiêu chí | Backup thủ công | Backup tự động | Restore thủ công | Restore tự động |
|---|---|---|---|---|
| Trigger | Nút “Sao lưu ngay” | Scheduler + chạy bù | Quản lý chọn bản + 2 bước | `HealthMonitor` phát hiện CRITICAL |
| Actor | `maNV` Quản lý | `SYSTEM` | `maNV` Quản lý | `SYSTEM` |
| Xác nhận | Không | Không | 2 bước + checkbox | Chính sách auto + rate limit |
| Chọn bản | Không áp dụng | Bản mới nhất theo lịch | Quản lý chọn | Bản hợp lệ mới nhất, fallback 3 bản |
| Safety backup | Không | Không | Có (`SAFETY`) | Có (`SAFETY`) |
| Loại lịch sử | `MANUAL` | `AUTO` | `MANUAL_RESTORE` | `AUTO_RESTORE` |
| UI | `ProgressDialog` + Toast | Tray/log | `ProgressDialog` + Toast | Tray + `IncidentDialog` |
| Rate limit | Không | Không | Không | 3 lần/giờ |
| Điều kiện dừng | — | `AppState != NORMAL` | — | Auto tắt / hết hạn mức / không có backup |

---

## 10. Cơ chế an toàn chung

| Cơ chế | Tác dụng |
|---|---|
| `MaintenanceLock` | Chỉ một backup/restore/health check tại một thời điểm |
| `pg_restore --single-transaction` | Restore all-or-nothing, lỗi giữa chừng rollback |
| `--clean --if-exists` | Xóa object cũ trước khi tạo lại, không lỗi nếu thiếu |
| SHA-256 + `pg_restore --list` | Không restore file hỏng/thiếu |
| Safety backup | Có thể quay lại trạng thái trước restore |
| Fallback 3 bản | Bản mới hỏng thì thử bản cũ hơn |
| Rate limit 3/giờ | Chống vòng lặp auto restore |
| `AppState` | Tạm dừng backup tự động khi dữ liệu bất thường |
| `SHARED` DataSource | DAO cũ sống lại sau khi pool được reset |
| File + `.meta.json` | Nguồn sự thật không phụ thuộc DB |
| `health_state.json` | Baseline/streak tồn tại ngoài DB |

---

## 11. Nhánh lỗi chính

| Tình huống | Xử lý |
|---|---|
| `pg_dump` lỗi / hết dung lượng | Xóa `.tmp`, ghi meta + lịch sử `FAILED`, thông báo |
| File backup hỏng / mất | `kiemTra` thất bại trước khi restore; fallback bản cũ hơn |
| `pg_restore` lỗi giữa chừng | `--single-transaction` rollback; ghi `FAILED`; thử bản kế tiếp nếu auto |
| DB không tồn tại | `createdb` rồi `pg_restore` lại một lần |
| Không có backup hợp lệ | Incident `FAILED`, cảnh báo, không ghi đè DB |
| Vượt 3 lần auto restore/giờ | Dừng auto, incident `FAILED`, yêu cầu can thiệp |
| PostgreSQL tắt / sai mật khẩu | `UNAVAILABLE`, không restore |
| Đang backup/restore khác | Bỏ qua nhờ `MaintenanceLock` |
| Quản lý bỏ qua incident | Baseline = số liệu hiện tại, incident `DISMISSED`, backup chạy lại |

---

## 12. Tham chiếu mã nguồn

| Thành phần | Tệp |
|---|---|
| Backup facade | `src/main/java/com/dtaq/retail/service/BackupService.java` |
| Restore facade | `src/main/java/com/dtaq/retail/service/RestoreService.java` |
| Lịch backup | `src/main/java/com/dtaq/retail/service/BackupScheduler.java` |
| Lưu file + metadata | `src/main/java/com/dtaq/retail/service/BackupStore.java` |
| Gọi pg_dump/pg_restore | `src/main/java/com/dtaq/retail/util/PgCommandExecutor.java` |
| Khóa bảo trì | `src/main/java/com/dtaq/retail/service/MaintenanceLock.java` |
| Health check | `src/main/java/com/dtaq/retail/service/DataHealthChecker.java` |
| Chính sách sự cố | `src/main/java/com/dtaq/retail/service/RecoveryCoordinator.java` |
| Giám sát định kỳ | `src/main/java/com/dtaq/retail/service/HealthMonitor.java` |
| Trạng thái ứng dụng | `src/main/java/com/dtaq/retail/service/AppState.java` |
| Cấu hình | `src/main/java/com/dtaq/retail/config/BackupConfig.java`, `HealthConfig.java` |
| UI sao lưu | `src/main/java/com/dtaq/retail/ui/page/backup/BackupPage.java` |
| UI phục hồi | `src/main/java/com/dtaq/retail/ui/page/backup/RestorePage.java` |
| Dialog sự cố | `src/main/java/com/dtaq/retail/ui/page/backup/IncidentDialog.java` |
| Migration | `src/main/resources/db/migration/V11__backup_history.sql`, `V12__lichsu_sao_luu_bo_rang_buoc_filepath.sql` |

---

## 13. Giới hạn đã biết

- **DB bị xóa lúc app đang tắt:** lần khởi động sau, Flyway migration chạy trước HealthMonitor và có thể dừng app trước khi auto restore kịp chạy. Auto restore cho `3D000` hiện chỉ chắc chắn khi app đang chạy.
- **`DROP TABLE` không có `CASCADE`:** PostgreSQL từ chối nếu còn khóa ngoại; cần `DROP TABLE ... CASCADE`.
- **`health_state.json` nằm trong `./backups`:** không được copy sang thư mục phụ; mất nếu xóa thư mục backup.
- **Backup chứa hash BCrypt:** không commit, không chia sẻ.
- **UI chưa có nút “Kiểm tra sức khỏe ngay”:** muốn test nhanh phải giảm `DTAQ_HEALTH_INTERVAL_MINUTES` hoặc khởi động lại app.
