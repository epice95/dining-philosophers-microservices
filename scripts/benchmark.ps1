$jar = "philosopher-service\target\philosopher-service-0.1.0-SNAPSHOT.jar"
$sizes = @(@(1000, 10000), @(10000, 100000), @(100000, 1000000))
$results = @()

foreach ($n in 1, 2, 4) {
    $urls = @()
    for ($i = 1; $i -le $n; $i++) {
        $port = 8090 + $i
        $env:PHILOSOPHER_ID = "worker-$i"
        $env:SERVER_PORT = "$port"
        $env:PHILOSOPHER_GRAPH_ROLE = "worker"
        Start-Process java -ArgumentList "-jar", $jar -WindowStyle Hidden
        $urls += "http://localhost:$port"
    }
    $env:PHILOSOPHER_ID = "master"
    $env:SERVER_PORT = "8081"
    $env:PHILOSOPHER_GRAPH_ROLE = "master"
    $env:PHILOSOPHER_GRAPH_WORKERS = ($urls -join ",")
    Start-Process java -ArgumentList "-jar", $jar -WindowStyle Hidden
    Remove-Item Env:PHILOSOPHER_ID, Env:SERVER_PORT, Env:PHILOSOPHER_GRAPH_ROLE, Env:PHILOSOPHER_GRAPH_WORKERS
    Start-Sleep -Seconds 15

    foreach ($s in $sizes) {
        $uri = "http://localhost:8081/graph/benchmark?nodes=$($s[0])&edges=$($s[1])&seed=42"
        Invoke-RestMethod -Method Post -Uri $uri | Out-Null
        $times = 1..5 | ForEach-Object { (Invoke-RestMethod -Method Post -Uri $uri).elapsedMillis }
        $median = ($times | Sort-Object)[2]
        $results += [pscustomobject]@{
            Workers  = $n
            Nodes    = $s[0]
            Edges    = $s[1]
            MedianMs = $median
        }
    }
    Stop-Process -Name java -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 3
}

$results | Format-Table
$results | Export-Csv benchmark.csv -NoTypeInformation