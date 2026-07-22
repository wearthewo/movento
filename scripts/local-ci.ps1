[CmdletBinding()]
param(
    [switch]$SkipDockerBuild
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$originalMavenOpts = $env:MAVEN_OPTS

function Invoke-Checked {
    param(
        [Parameter(Mandatory)] [string]$FilePath,
        [Parameter()] [string[]]$ArgumentList = @()
    )

    & $FilePath @ArgumentList
    if ($LASTEXITCODE -ne 0) {
        throw "$FilePath exited with code $LASTEXITCODE"
    }
}

try {
    Set-Location $repoRoot

    $certificates = @(Get-ChildItem -LiteralPath (Join-Path $repoRoot ".local-certs") -Filter "*.pem" -File -ErrorAction SilentlyContinue)
    if ($certificates.Count -gt 0) {
        if (-not $env:JAVA_HOME) {
            throw "JAVA_HOME must be set to build a local truststore."
        }

        $sourceTruststore = Join-Path $env:JAVA_HOME "lib\security\cacerts"
        $keytool = Join-Path $env:JAVA_HOME "bin\keytool.exe"
        if (-not (Test-Path -LiteralPath $sourceTruststore) -or -not (Test-Path -LiteralPath $keytool)) {
            throw "JAVA_HOME does not point to a complete JDK: $env:JAVA_HOME"
        }

        $localTools = Join-Path $repoRoot ".local-tools"
        $truststore = Join-Path $localTools "movento-ci-cacerts"
        New-Item -ItemType Directory -Force -Path $localTools | Out-Null
        Copy-Item -Force -LiteralPath $sourceTruststore -Destination $truststore

        foreach ($certificate in $certificates) {
            $alias = "movento-local-" + $certificate.BaseName.ToLowerInvariant()
            Invoke-Checked $keytool @(
                "-importcert", "-noprompt", "-trustcacerts",
                "-alias", $alias,
                "-file", $certificate.FullName,
                "-keystore", $truststore,
                "-storepass", "changeit"
            )
        }

        $env:MAVEN_OPTS = "$originalMavenOpts -Djavax.net.ssl.trustStore=$truststore -Djavax.net.ssl.trustStorePassword=changeit".Trim()
    }

    Invoke-Checked (Join-Path $repoRoot "mvnw.cmd") @("--batch-mode", "verify")

    Invoke-Checked "docker" @("compose", "config", "--quiet")
    Invoke-Checked "docker" @("compose", "build", "web")
    if (-not $SkipDockerBuild) {
        Invoke-Checked "docker" @("compose", "build")
    }

    Write-Host "Movento local CI passed." -ForegroundColor Green
}
finally {
    $env:MAVEN_OPTS = $originalMavenOpts
    Set-Location $repoRoot
}
