$tomcat = "D:\moniC\project\learn\.tools\apache-tomcat-9.0.118"
$env:CATALINA_HOME = $tomcat
$env:CATALINA_BASE = $tomcat
$env:JAVA_HOME = "D:\Program Files\Java\jdk-21"

Start-Process -FilePath (Join-Path $tomcat "bin\startup.bat") `
    -WorkingDirectory (Join-Path $tomcat "bin") `
    -WindowStyle Hidden

Write-Host "Tomcat starting..."
Write-Host "Open: http://localhost:8080/JSPTest/Ex11_1.jsp"
