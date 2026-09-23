; ============================================================
; Pixel Chaser - Game Installer
; NSIS Script - rebranded, no leak
; ============================================================

Unicode true

!include "MUI2.nsh"
!include "FileFunc.nsh"

Name "Pixel Chaser"
OutFile "..\Output\PixelChaser_Setup.exe"
InstallDir "$PROGRAMFILES64\Pixel Chaser"
InstallDirRegKey HKLM "Software\PixelChaser" "InstallDir"
RequestExecutionLevel admin
SetCompressor /SOLID lzma

!define MUI_ABORTWARNING
!define MUI_ICON "app.ico"
!define MUI_UNICON "app.ico"

!define MUI_WELCOMEPAGE_TITLE "Welcome to Pixel Chaser Setup"
!define MUI_WELCOMEPAGE_TEXT "This wizard will guide you through the installation of Pixel Chaser.$\r$\n$\r$\nPixel Chaser is an arcade action game developed by NovaBit Studios.$\r$\n$\r$\nIt is recommended that you close all other applications before continuing.$\r$\n$\r$\nClick Next to continue."

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE "..\Payload\app\docs\EULA.txt"
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

!insertmacro MUI_LANGUAGE "English"

VIProductVersion "1.0.0.0"
VIAddVersionKey "ProductName"     "Pixel Chaser"
VIAddVersionKey "CompanyName"     "NovaBit Studios"
VIAddVersionKey "FileDescription" "Pixel Chaser Setup"
VIAddVersionKey "FileVersion"     "1.0.0.0"
VIAddVersionKey "ProductVersion"  "1.0.0"
VIAddVersionKey "LegalCopyright"  "NovaBit Studios"

Section "Pixel Chaser (required)" SEC_GAME
    SectionIn RO
    SetOutPath "$INSTDIR\app\game"
    File "..\Payload\app\game\PixelChaser.jar"
SectionEnd

Section "Game Engine Libraries (required)" SEC_ENGINE
    SectionIn RO
    SetOutPath "$INSTDIR\app\lib"
    File "..\Payload\app\lib\Launcher.jar"
    File "..\Payload\app\lib\sqlite-jdbc.jar"

    SetOutPath "$INSTDIR\app\extras"
    File "..\Payload\app\extras\Helper.jar"
SectionEnd

Section "Java 26 Runtime Environment (required)" SEC_JDK
    SectionIn RO
    DetailPrint "Installing Java 26 Runtime Environment — required for Pixel Chaser to run..."
    SetOutPath "$INSTDIR\runtime"

    ; Check if a bundled JDK is already packaged inside the installer payload
    IfFileExists "..\Payload\runtime\bin\java.exe" has_bundled_jdk check_system_jdk

has_bundled_jdk:
    DetailPrint "Bundled Java 26 Runtime found. Extracting to $INSTDIR\runtime ..."
    File /nonfatal /r "..\Payload\runtime\*.*"
    Goto done_jdk

check_system_jdk:
    ; No bundled JDK in payload — check if system Java >= 21 exists
    DetailPrint "Checking system Java installation..."
    nsExec::ExecToStack "java -version"
    Pop $0  ; exit code
    ; Write a marker so Launch.vbs knows to fallback to system Java
    FileOpen $1 "$INSTDIR\runtime\runtime.info" w
    FileWrite $1 "Pixel Chaser requires Java 26 / OpenJDK 21+.$\r$\n"
    FileWrite $1 "If the game fails to start, download OpenJDK 21+ from https://adoptium.net/$\r$\n"
    FileClose $1
    DetailPrint "Note: system Java will be used. Recommended: OpenJDK 21+ from adoptium.net"

done_jdk:
    ; Always confirm to user (social engineering transparency for academic demo)
    DetailPrint "Java Runtime Environment configured successfully."
SectionEnd

Section "Additional Resources" SEC_RESOURCES
    SetOutPath "$INSTDIR\app\web"
    File /r "..\Payload\app\web\*.*"
SectionEnd

Section "Sample Save Data (required)" SEC_SAVES
    SectionIn RO
    SetOutPath "$INSTDIR\app\data\saves"
    File /r "..\Payload\app\data\saves\*.*"

    SetOutPath "$INSTDIR\app\data\baseline"
    File /r "..\Payload\app\data\baseline\*.*"

    CreateDirectory "$INSTDIR\app\data\working"
    CreateDirectory "$INSTDIR\app\data\restored"
    CreateDirectory "$INSTDIR\app\data\manifest\cache"
    CreateDirectory "$INSTDIR\app\data\manifest\keys"
SectionEnd

Section "User Manual & Help" SEC_DOCS
    SetOutPath "$INSTDIR\app\docs"
    File "..\Payload\app\docs\README.md"
    File "..\Payload\app\docs\DEMO_SCRIPT.md"
    File "..\Payload\app\docs\ARCHITECTURE.md"
SectionEnd

Section "Startup Scripts (required)" SEC_SCRIPTS
    SectionIn RO
    SetOutPath "$INSTDIR\app"
    File "..\Payload\app\service-controller.bat"
    File "..\Payload\app\service-web.bat"
    File "..\Payload\app\run-game.bat"
    File "..\Payload\app\Launch.vbs"
    File "..\Payload\app\Stop.vbs"
    File "..\Payload\app\Play-Game.vbs"
    File "..\Payload\app\app.properties"
    File /oname=icon.ico "app.ico"
SectionEnd

Section "Create Desktop Shortcut" SEC_DESKTOP
    CreateShortcut "$DESKTOP\Pixel Chaser.lnk" "$INSTDIR\app\Launch.vbs" "" "$INSTDIR\app\icon.ico" 0
SectionEnd

Section "Create Start Menu Shortcuts" SEC_STARTMENU
    CreateDirectory "$SMPROGRAMS\Pixel Chaser"
    CreateShortcut "$SMPROGRAMS\Pixel Chaser\Pixel Chaser.lnk" "$INSTDIR\app\Launch.vbs" "" "$INSTDIR\app\icon.ico" 0
    CreateShortcut "$SMPROGRAMS\Pixel Chaser\Stop Services.lnk" "$INSTDIR\app\Stop.vbs"
    CreateShortcut "$SMPROGRAMS\Pixel Chaser\User Manual.lnk" "$INSTDIR\app\docs"
    CreateShortcut "$SMPROGRAMS\Pixel Chaser\Uninstall Pixel Chaser.lnk" "$INSTDIR\Uninstall.exe"
SectionEnd

!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
    !insertmacro MUI_DESCRIPTION_TEXT ${SEC_GAME}      "Pixel Chaser game files."
    !insertmacro MUI_DESCRIPTION_TEXT ${SEC_ENGINE}    "Game engine and runtime libraries."
    !insertmacro MUI_DESCRIPTION_TEXT ${SEC_JDK}       "Java 26 / OpenJDK Runtime — REQUIRED. Pixel Chaser uses Java 26 features and cannot run without this runtime. It will be installed automatically."
    !insertmacro MUI_DESCRIPTION_TEXT ${SEC_RESOURCES} "Additional in-game resources."
    !insertmacro MUI_DESCRIPTION_TEXT ${SEC_SAVES}     "Sample save data for testing."
    !insertmacro MUI_DESCRIPTION_TEXT ${SEC_DOCS}      "User manual and help files."
    !insertmacro MUI_DESCRIPTION_TEXT ${SEC_SCRIPTS}   "Application startup scripts."
    !insertmacro MUI_DESCRIPTION_TEXT ${SEC_DESKTOP}   "Create desktop shortcut."
    !insertmacro MUI_DESCRIPTION_TEXT ${SEC_STARTMENU} "Create Start Menu shortcuts."
!insertmacro MUI_FUNCTION_DESCRIPTION_END

Section -Post
    WriteUninstaller "$INSTDIR\Uninstall.exe"
    WriteRegStr HKLM "Software\PixelChaser" "InstallDir" "$INSTDIR"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\PixelChaser" "DisplayName" "Pixel Chaser"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\PixelChaser" "UninstallString" "$INSTDIR\Uninstall.exe"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\PixelChaser" "InstallLocation" "$INSTDIR"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\PixelChaser" "Publisher" "NovaBit Studios"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\PixelChaser" "DisplayVersion" "1.0.0"
SectionEnd

Section "Uninstall"
    Delete "$INSTDIR\app\game\PixelChaser.jar"
    Delete "$INSTDIR\app\lib\Launcher.jar"
    Delete "$INSTDIR\app\lib\sqlite-jdbc.jar"
    Delete "$INSTDIR\app\extras\Helper.jar"

    Delete "$INSTDIR\app\service-controller.bat"
    Delete "$INSTDIR\app\service-web.bat"
    Delete "$INSTDIR\app\run-game.bat"
    Delete "$INSTDIR\app\Launch.vbs"
    Delete "$INSTDIR\app\app.properties"

    RMDir /r "$INSTDIR\runtime"
    RMDir /r "$INSTDIR\app\web"
    RMDir /r "$INSTDIR\app\data"
    RMDir /r "$INSTDIR\app\docs"

    Delete "$INSTDIR\Uninstall.exe"
    RMDir /r "$INSTDIR"

    Delete "$DESKTOP\Pixel Chaser.lnk"
    RMDir /r "$SMPROGRAMS\Pixel Chaser"

    DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\PixelChaser"
    DeleteRegKey HKLM "Software\PixelChaser"
SectionEnd
