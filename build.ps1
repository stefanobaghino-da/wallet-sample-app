Set-Location main/User
daml build -o user.dar
Write-Output "User dar file built"

Set-Location ../Asset
daml build -o asset.dar
Write-Output "Asset dar file built"

Set-Location ../Account
daml build -o account.dar
daml build -o setup.dar

Write-Output "Account dar file built"
Write-Output "Setup dar file built"

Set-Location ../../triggers
daml build -o triggers.dar
Write-Output "Triggers dar file built"
Write-Output "Completed building dar files"

Set-Location ..