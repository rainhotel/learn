$tomcat = "D:\moniC\project\learn\.tools\apache-tomcat-9.0.118"
$env:CATALINA_HOME = $tomcat
$env:CATALINA_BASE = $tomcat
$env:JAVA_HOME = "D:\Program Files\Java\jdk-21"

& (Join-Path $tomcat "bin\shutdown.bat")

Write-Host "Tomcat stop command sent."
