param()

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Split-Path -Parent $ScriptDir
$EnvFile = Join-Path $RootDir '.env'

function Write-EnvVariable {
    param(
        [Parameter(Mandatory = $true)] [string] $Name,
        [Parameter(Mandatory = $true)] [string] $Value
    )

    [System.Environment]::SetEnvironmentVariable($Name, $Value, 'Process')
    Set-Item -Path ("Env:{0}" -f $Name) -Value $Value
}

if (Test-Path $EnvFile) {
    Write-Host 'Loading environment from .env'

    Get-Content $EnvFile | ForEach-Object {
        $line = $_.Trim()

        if (-not [string]::IsNullOrWhiteSpace($line) -and -not $line.StartsWith('#')) {
            $equalsIndex = $line.IndexOf('=')
            if ($equalsIndex -ge 1) {
                $name = $line.Substring(0, $equalsIndex).Trim()
                $value = $line.Substring($equalsIndex + 1).Trim()

                if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
                    $value = $value.Substring(1, $value.Length - 2)
                }

                Write-EnvVariable -Name $name -Value $value
            }
        }
    }
}
else {
    Write-Warning 'Warning: .env not found in project root. Relying on environment variables.'
}

if ([string]::IsNullOrWhiteSpace($env:SPRING_PROFILES_ACTIVE)) {
    Write-EnvVariable -Name 'SPRING_PROFILES_ACTIVE' -Value 'supabase'
}

Write-Host ("Active Spring profile: {0}" -f $env:SPRING_PROFILES_ACTIVE)

Push-Location $RootDir
try {
    & mvn -f (Join-Path $RootDir 'pom.xml') spring-boot:run
}
finally {
    Pop-Location
}