# Deletes all locally built files

Write-Output "Removing dar files..."

Set-Location main/Account
Remove-Item -Recurse -Force .daml
Remove-Item -Force *.dar

Set-Location ../Asset
Remove-Item -Recurse -Force .daml
Remove-Item -Force *.dar 

Set-Location ../User
Remove-Item -Recurse -Force .daml
Remove-Item -Recurse *.dar 

Set-Location ../../triggers
Remove-Item -Recurse -Force .daml
Remove-Item -Recurse *.dar

Set-Location ../ui
Write-Output "Removing Node modules. This could take a while. Please wait..."
Remove-Item -Recurse -Force node_modules 
Write-Output "Remiving UI build files..."
Remove-Item -Recurse -Force build 
Write-Output "Removing daml.js"
Remove-Item -Recurse -Force daml.js 
Set-Location ..