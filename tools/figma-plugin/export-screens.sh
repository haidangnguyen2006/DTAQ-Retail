#!/usr/bin/env sh
# Xuất ảnh mọi màn hình (2x) + screens.json cho plugin Figma.
# Cần PostgreSQL đang chạy (db.properties) và môi trường có màn hình đồ họa. Chỉ đọc dữ liệu.
set -e
cd "$(dirname "$0")/../.."
mvn -q -DskipTests package
J2D_UISCALE=1 java -Dsun.java2d.uiScale=1 \
  -cp "target/test-classes:target/dtaq-retail-1.0.0-SNAPSHOT.jar" \
  com.dtaq.retail.ui.FigmaExport tools/figma-plugin/screens "$@"
