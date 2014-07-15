

# From Cygwin...
#
# ANT_HOME=/cygdrive/c/Program\ Files/apache-ant-1.9.4
# JAVA_HOME=/cygdrive/c/Program\ Files/Java/jdk1.7.0_13
# PATH=${JAVA_HOME}/bin:${ANT_HOME}/bin:/cygdrive/c/Program\ Files\ \(x86\)/Google/adt-bundle-windows-x86_64/sdk/tools:${PATH}
# ANDROID_HOME=/cygdrive/c/Program\ Files\ \(x86\)/Google/adt-bundle-windows-x86_64/sdk


#
# FUNCTIONS
#

function WriteHostFormatted ($msg)
{
  Write-Host "* $($msg)"
}


#
# CONSTS
#
$antBuildScript = "build.xml"
$keystorePath = "my-release-key.keystore"
$keyalg = "RSA"
$keysize = 4096
$keyvalidity = 10000



#
# MAIN
#

# Check env variable existence.

if($env:JAVA_HOME -eq $null) {
  $env:JAVA_HOME = "C:\Program Files\Java\jdk1.7.0_13"
}
if($env:ANT_HOME -eq $null) {
  $env:ANT_HOME = "C:\Program Files\apache-ant-1.9.4"
}
if($env:ANDROID_HOME -eq $null) {
  $env:ANDROID_HOME = "C:\Program Files (x86)\Google\adt-bundle-windows-x86_64\sdk"
}
WriteHostFormatted("Variables are set:")
Write-Host "  JAVA_HOME=$($env:JAVA_HOME)"
Write-Host "  ANT_HOME=$($env:ANT_HOME)"
Write-Host "  ANDROID_HOME=$($env:ANDROID_HOME)"



# Create the signing key.
if(-Not (Test-Path $keystorePath)) {
  WriteHostFormatted("Generating Purple Robot signing key...")
  & "$($env:JAVA_HOME)\bin\keytool.exe" -genkey -v -keystore $keystorePath -alias alias_name -keyalg $keyalg -keysize $keysize -validity $keyvalidity
}
else {
  WriteHostFormatted("Purple Robot signing key already exists; not generating a new one.")
}



# Perform a PR release build.
WriteHostFormatted("Running the Ant build...")
& "$($env:ANT_HOME)\bin\ant.bat" clean release


