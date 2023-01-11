#!/bin/sh

aui_root_path=$1
target_root_path=$2
business_dir_name=AUIInteractionBusiness
business_dir=$aui_root_path/$business_dir_name

setting_gradle=$target_root_path/settings.gradle
aui_dir_name=AUIInteractionLive
target_aui_dir=$target_root_path/$aui_dir_name
target_business_dir=$target_aui_dir/$business_dir_name

echo "aui_root_path=$aui_root_path"
echo "business_dir=$business_dir"
echo "target_root_path=$target_root_path"
echo "setting_gradle=$setting_gradle"
echo "target_aui_dir=$target_aui_dir"

echo "import start..."

mkdir -p $target_business_dir

echo "copy AUIInteractionCore..."
rm -rf $aui_root_path/AUIInteractionCore/build
cp -rf $aui_root_path/AUIInteractionCore $target_aui_dir/AUIInteractionCore

echo "copy AUIInteractionLiveRoom..."
rm -rf $aui_root_path/AUIInteractionLiveRoom/build
cp -rf $aui_root_path/AUIInteractionLiveRoom $target_aui_dir/AUIInteractionLiveRoom

echo "copy AUIInteractionUikit..."
rm -rf $aui_root_path/AUIInteractionUikit/build
cp -rf $aui_root_path/AUIInteractionUikit $target_aui_dir/AUIInteractionUikit

echo "copy AUIInteractionBeauty..."
rm -rf $aui_root_path/AUIInteractionBeauty/build
cp -rf $aui_root_path/AUIInteractionBeauty $target_aui_dir/AUIInteractionBeauty

echo "copy AUICommon..."
rm -rf $business_dir/AUICommon/build
cp -rf $business_dir/AUICommon $target_business_dir/AUICommon

echo "copy AUIMessage..."
rm -rf $business_dir/AUIMessage/build
cp -rf $business_dir/AUIMessage $target_business_dir/AUIMessage

echo "copy AUIPlayer..."
rm -rf $business_dir/AUIPlayer/build
cp -rf $business_dir/AUIPlayer $target_business_dir/AUIPlayer

echo "\n" >> $target_root_path/settings.gradle
echo "include ':AUIInteractionCore'" >> $target_root_path/settings.gradle
echo "project(':AUIInteractionCore').projectDir = new File(\"$aui_dir_name/AUIInteractionCore\")\n" >> $target_root_path/settings.gradle

echo "include ':AUIInteractionLiveRoom'" >> $target_root_path/settings.gradle
echo "project(':AUIInteractionLiveRoom').projectDir = new File(\"$aui_dir_name/AUIInteractionLiveRoom\")\n" >> $target_root_path/settings.gradle

echo "include ':AUIInteractionUikit'" >> $target_root_path/settings.gradle
echo "project(':AUIInteractionUikit').projectDir = new File(\"$aui_dir_name/AUIInteractionUikit\")\n" >> $target_root_path/settings.gradle

echo "include ':AUIInteractionBeauty'" >> $target_root_path/settings.gradle
echo "project(':AUIInteractionBeauty').projectDir = new File(\"$aui_dir_name/AUIInteractionBeauty\")\n" >> $target_root_path/settings.gradle

echo "include ':$business_dir_name:AUICommon'" >> $target_root_path/settings.gradle
echo "project(':$business_dir_name:AUICommon').projectDir = new File(\"$aui_dir_name/$business_dir_name/AUICommon\")\n" >> $target_root_path/settings.gradle

echo "include ':$business_dir_name:AUIMessage'" >> $target_root_path/settings.gradle
echo "project(':$business_dir_name:AUIMessage').projectDir = new File(\"$aui_dir_name/$business_dir_name/AUIMessage\")\n" >> $target_root_path/settings.gradle

echo "include ':$business_dir_name:AUIPlayer'" >> $target_root_path/settings.gradle
echo "project(':$business_dir_name:AUIPlayer').projectDir = new File(\"$aui_dir_name/$business_dir_name/AUIPlayer\")\n" >> $target_root_path/settings.gradle

sed -i '' "/.*repositories.*/a\\
        maven { \\
            allowInsecureProtocol = true \\
            url \"https:\/\/maven.aliyun.com\/nexus\/content\/repositories\/releases\" \\
        }
" $target_root_path/build.gradle

echo "...done!!! finish"