' ============================================================
' Pixel Chaser - Attacker C2 Portal Launcher
' Opens Attacker C2 Console in a SEPARATE WINDOW at game launch
' (Victim Portal is NOT opened automatically; victim opens it manually via LockScreen)
' ============================================================
Option Explicit

Dim shell, fso, rid, attackerUrl, browserExe, flag

Set shell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

' Optional parameter: recoveryId
rid = ""
If WScript.Arguments.Count > 0 Then
    rid = Trim(WScript.Arguments(0))
End If

attackerUrl = "http://127.0.0.1:8080/c2.html"
If rid <> "" Then
    attackerUrl = "http://127.0.0.1:8080/c2.html?rid=" & rid
End If

' Detect default browser executable
Function GetBrowserExe()
    Dim cmd, p1, p2, candidate, pid
    cmd = ""
    On Error Resume Next
    pid = shell.RegRead("HKCU\Software\Microsoft\Windows\Shell\Associations\UrlAssociations\http\UserChoice\ProgId")
    If pid <> "" Then
        cmd = shell.RegRead("HKCR\" & pid & "\shell\open\command\")
    End If
    On Error Goto 0

    If cmd <> "" Then
        If Left(cmd, 1) = """" Then
            p2 = InStr(2, cmd, """")
            If p2 > 2 Then
                candidate = Mid(cmd, 2, p2 - 2)
                If fso.FileExists(candidate) Then
                    GetBrowserExe = candidate
                    Exit Function
                End If
            End If
        Else
            p1 = InStr(cmd, " ")
            If p1 > 0 Then
                candidate = Left(cmd, p1 - 1)
            Else
                candidate = cmd
            End If
            If fso.FileExists(candidate) Then
                GetBrowserExe = candidate
                Exit Function
            End If
        End If
    End If

    ' Fallbacks for common Windows browsers
    Dim fallbacks, f
    fallbacks = Array( _
        "C:\Program Files\BraveSoftware\Brave-Browser\Application\brave.exe", _
        "C:\Program Files\Google\Chrome\Application\chrome.exe", _
        "C:\Program Files (x86)\Google\Chrome\Application\chrome.exe", _
        shell.ExpandEnvironmentStrings("%LOCALAPPDATA%\Google\Chrome\Application\chrome.exe"), _
        "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe", _
        "C:\Program Files\Microsoft\Edge\Application\msedge.exe" _
    )
    For Each f In fallbacks
        If fso.FileExists(f) Then
            GetBrowserExe = f
            Exit Function
        End If
    Next
    GetBrowserExe = ""
End Function

browserExe = GetBrowserExe()

If browserExe <> "" Then
    If InStr(LCase(browserExe), "firefox") > 0 Then
        flag = "-new-window"
    Else
        flag = "--new-window"
    End If

    ' Launch Attacker C2 Dashboard in its own window
    shell.Run """" & browserExe & """ " & flag & " """ & attackerUrl & """", 1, False
Else
    ' Fallback to standard URL associations if no browser executable detected
    shell.Run attackerUrl, 1, False
End If
