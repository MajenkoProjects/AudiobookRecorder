#!/bin/bash

NAME=AudiobookRecorder

BIN=/usr/bin
DESKTOP=/usr/share/applications
SHARE=/usr/share
ICON=/usr/share/icons


mkdir -p "$BIN"
mkdir -p "$SHARE/$NAME"

cp AudiobookRecorder.jar "$SHARE/$NAME/$NAME.jar"
echo "#!/bin/bash" > "$BIN/$NAME"
echo "java -jar \"$SHARE/$NAME/$NAME.jar\"" >> "$BIN/$NAME"
chmod 755 "$BIN/$NAME"

echo "[Desktop Entry]" > "$DESKTOP/$NAME.desktop"
echo "Version=1.0" >> "$DESKTOP/$NAME.desktop"
echo "Name=$NAME" >> "$DESKTOP/$NAME.desktop"
echo "Exec=$NAME" >> "$DESKTOP/$NAME.desktop"
echo "Icon=$NAME" >> "$DESKTOP/$NAME.desktop"
echo "Categories=Multimedia" >> "$DESKTOP/$NAME.desktop"

cp resources/uk/co/majenko/audiobookrecorder/icons/appIcon.png "$ICON/$NAME.png"
