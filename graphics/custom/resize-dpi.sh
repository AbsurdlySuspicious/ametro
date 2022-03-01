#!/usr/bin/env bash

res_n=("xxxhdpi" "xxhdpi" "xhdpi" "hdpi" "mdpi")
res_s=(4 3 2 1.5 1)

echo "resize-dpi.sh [-d] {INPUT} {OUT_DIR}"
echo "INPUT is assumed to be ${res_n[0]}"
echo "-d is drawable out mode"
echo

args=()
out_mode="single"
while [[ $# -gt 0 ]]; do
  case "$1" in
    "-d") out_mode="drawable" ;;
    *) args+=("$1") ;;
  esac; shift 1
done

ai=${args[0]}; [ -f "$ai" ] || { echo "no input file"; exit 1; }
ao=${args[1]}; [ -d "$ao" ] || { echo "out dir is not a directory"; exit 1; }

init_res=$(identify "$ai" | grep -Po '(?<=\s|^)(\d+x\d+)(?=\s|$)')
init_w=${init_res%x*}
init_h=${init_res#*x}
init_scale=${res_s[0]}

for i in "${!res_n[@]}"; do
  base=$(basename "$ai")
  case "$out_mode" in
    "single") out="$ao/${base%.*}-${res_n[$i]}.${base##*.}" ;;
    "drawable") d="$ao/drawable-${res_n[$i]}"; out="$d/$base"; mkdir "$d" ;;
    *) echo "unknown out mode '$out_mode'"; exit 1 ;;
  esac
  echo "$ai -> $out"

  if [[ $i == 0 ]]; then
    cp "$ai" "$out"
    continue
  fi

  new_w=$(bc <<<"scale=0; $init_w / $init_scale * ${res_s[$i]} / 1")
  new_h=$(bc <<<"scale=0; $init_h / $init_scale * ${res_s[$i]} / 1")
  convert "$ai" -resize "${new_w}x${new_h}" "$out"
done
