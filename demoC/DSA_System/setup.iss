[Setup]
AppName=DSA Security
AppVersion=1.0
DefaultDirName={pf}\DSA Security
DefaultGroupName=DSA Security
OutputDir=output
OutputBaseFilename=DSA_Setup
Compression=lzma
SolidCompression=yes
WizardStyle=modern

[Files]
Source: "D:\DocumentOnClass\2025_2026\Ky_2\AnNinhMang\BaoCao\demoC\DSA_System\build\Desktop_Qt_6_11_1_MinGW_64_bit-Debug\*"; DestDir: "{app}"; Flags: recursesubdirs

[Icons]
Name: "{group}\DSA Security"; Filename: "{app}\DSA_System.exe"
Name: "{autodesktop}\DSA Security"; Filename: "{app}\DSA_System.exe"

[Run]
Filename: "{app}\DSA_System.exe"; Description: "Run DSA Security"; Flags: nowait postinstall skipifsilent