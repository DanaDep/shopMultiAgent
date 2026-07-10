<#
.SYNOPSIS
    Derives the ground truth for evals/questions.json from the mock data files.

.DESCRIPTION
    Reads src/main/resources/mock/*.json and recomputes the expected answer for each
    eval question, printing the full derivation (which records, which filter, what sum)
    so a human can verify by reading instead of re-tracing JSON by hand.

    Date-window logic deliberately mirrors the app's tools: LocalDate.now().minusX()
    with an EXCLUSIVE isAfter() comparison. Where a tool's logic is questionable
    (top sellers counting CANCELLED orders), both the tool's answer and the
    business-correct answer are printed and the difference is flagged as QUIRK.

.PARAMETER Ids
    Only derive these question ids, e.g. -Ids Q06,Q07

.PARAMETER AsOf
    Compute date windows as if today were this date (yyyy-MM-dd). Defaults to today.
    Useful to reproduce a past run or to preview when answers will drift.

.EXAMPLE
    .\derive-ground-truth.ps1
    .\derive-ground-truth.ps1 -Ids Q06,Q07 -AsOf 2026-09-04
#>
param(
    [string[]]$Ids,
    [string]$AsOf
)

$ErrorActionPreference = 'Stop'

if ($AsOf) { $today = [datetime]::ParseExact($AsOf, 'yyyy-MM-dd', $null) }
else       { $today = (Get-Date).Date }

$mockDir = Join-Path $PSScriptRoot '..\src\main\resources\mock'
$orders  = @((Get-Content (Join-Path $mockDir 'orders.json')  -Raw | ConvertFrom-Json))
$returns = @((Get-Content (Join-Path $mockDir 'returns.json') -Raw | ConvertFrom-Json))
$refunds = @((Get-Content (Join-Path $mockDir 'refunds.json') -Raw | ConvertFrom-Json))
$reviews = @((Get-Content (Join-Path $mockDir 'reviews.json') -Raw | ConvertFrom-Json))

function ParseDate([string]$d) { [datetime]::ParseExact($d, 'yyyy-MM-dd', $null) }
function Money([decimal]$m)    { '${0:N2}' -f $m }

$script:currentId = $null
function Section([string]$id, [string]$question) {
    $script:currentId = $id
    if ($Ids -and $Ids -notcontains $id) { return $false }
    Write-Host ''
    Write-Host "=== $id`: $question ===" -ForegroundColor Cyan
    return $true
}
function Answer([string]$text) { Write-Host "GROUND TRUTH: $text" -ForegroundColor Green }
function Quirk([string]$text)  { Write-Host "!! QUIRK: $text" -ForegroundColor Yellow }

# Shared aggregate: average rating per product (mirrors ReviewTool.getAverageRatingPerProduct)
$ratings = $reviews | Group-Object productName | ForEach-Object {
    [pscustomobject]@{
        Product = $_.Name
        Avg     = [math]::Round(($_.Group | Measure-Object rating -Average).Average, 2)
        Count   = $_.Count
    }
}

Write-Host "Deriving ground truth from $mockDir"
Write-Host "As of: $($today.ToString('yyyy-MM-dd'))  (date windows mirror the tools: strictly AFTER today minus window)"
Write-Host "Data: $($orders.Count) orders, $($returns.Count) returns, $($refunds.Count) refunds, $($reviews.Count) reviews"

# ---------------------------------------------------------------------------
if (Section 'Q01' 'Which product gets returned the most, and what are the main reasons?') {
    # mirrors ReturnTool.getMostReturnedProduct (count per product, no date filter)
    $byProduct = $returns | Group-Object productName | Sort-Object Count -Descending
    foreach ($g in $byProduct) {
        $reasons = ($g.Group | Group-Object reason | Sort-Object Count -Descending |
                    ForEach-Object { "$($_.Count)x '$($_.Name)'" }) -join ', '
        Write-Host ("  {0,-22} {1} return(s): {2}   [{3}]" -f $g.Name, $g.Count, $reasons, (($g.Group.id) -join ', '))
    }
    $top = $byProduct[0]
    $topReasons = ($top.Group | Group-Object reason | Sort-Object Count -Descending |
                   ForEach-Object { "$($_.Count)x '$($_.Name)'" }) -join ', '
    Answer "$($top.Name), $($top.Count) returns: $topReasons"
}

# ---------------------------------------------------------------------------
if (Section 'Q02' 'What is our most refunded product?') {
    # mirrors RefundTool.getMostRefundedProduct (count per product, no date filter)
    $byProduct = $refunds | Group-Object productName | Sort-Object Count -Descending
    foreach ($g in $byProduct) {
        $reasons = ($g.Group | Group-Object reason | Sort-Object Count -Descending |
                    ForEach-Object { "$($_.Count)x '$($_.Name)'" }) -join ', '
        Write-Host ("  {0,-22} {1} refund(s): {2}   [{3}]" -f $g.Name, $g.Count, $reasons, (($g.Group.id) -join ', '))
    }
    $top = $byProduct[0]
    Answer "$($top.Name), $($top.Count) refunds"
}

# ---------------------------------------------------------------------------
if (Section 'Q03' 'What are our top 3 best-rated products?') {
    # mirrors ReviewTool.getBestRatedProducts (avg per product, top 3)
    $sorted = $ratings | Sort-Object Avg -Descending
    foreach ($r in $sorted) { Write-Host ("  {0,-22} avg {1}  ({2} review(s))" -f $r.Product, $r.Avg, $r.Count) }
    $top3 = $sorted | Select-Object -First 3
    $maxAvg = $sorted[0].Avg
    $tied = @($sorted | Where-Object { $_.Avg -eq $maxAvg })
    if ($tied.Count -gt 1) {
        Write-Host "  NOTE: $($tied.Count) products tie at $maxAvg - their relative order is arbitrary."
    }
    Answer (($top3 | ForEach-Object { "$($_.Product) ($($_.Avg), $($_.Count) reviews)" }) -join '; ')
}

# ---------------------------------------------------------------------------
if (Section 'Q04' 'Which products have the worst customer ratings?') {
    # mirrors ReviewTool.getWorstRatedProducts (avg per product, bottom 3)
    $bottom3 = $ratings | Sort-Object Avg | Select-Object -First 3
    foreach ($r in $bottom3) { Write-Host ("  {0,-22} avg {1}  ({2} review(s))" -f $r.Product, $r.Avg, $r.Count) }
    Answer (($bottom3 | ForEach-Object { "$($_.Product) ($($_.Avg))" }) -join '; ')
}

# ---------------------------------------------------------------------------
if (Section 'Q05' 'What was the most expensive order ever placed?') {
    # mirrors OrderTool.getMostExpensiveOrder: max by the per-order 'amount' field only
    $maxAmount = ($orders | Measure-Object amount -Maximum).Maximum
    $tied = @($orders | Where-Object { $_.amount -eq $maxAmount })
    Write-Host "  Max 'amount' field: $(Money $maxAmount) - order(s) at that amount:"
    foreach ($o in $tied) {
        Write-Host ("    {0}  {1,-12} qty {2}  {3}  {4}" -f $o.id, $o.productName, $o.quantity, $o.date, $o.status)
    }
    Quirk "The tool compares the raw 'amount' field, NOT amount x quantity, and returns ONE arbitrary order from the tie."
    if ($tied | Where-Object { $_.status -eq 'CANCELLED' }) {
        Quirk 'The tie includes a CANCELLED order - the tool does not filter by status.'
    }
    Answer "A Smart Watch order at $(Money $maxAmount) ($($tied.Count) orders tie; certainty about WHICH one is unwarranted)"
}

# ---------------------------------------------------------------------------
if (Section 'Q06' 'How much money did we refund in total over the last year?') {
    # mirrors RefundTool.getTotalRefundAmountLastYear: date strictly after today-1y
    $start = $today.AddYears(-1)
    Write-Host "  Window: strictly after $($start.ToString('yyyy-MM-dd'))"
    $total = [decimal]0
    foreach ($r in $refunds) {
        $in = (ParseDate $r.date) -gt $start
        if ($in) { $total += [decimal]$r.amount; $mark = 'IN ' } else { $mark = 'OUT' }
        Write-Host ("    {0}  {1}  {2,-22} {3,9}  {4}" -f $r.id, $r.date, $r.productName, (Money $r.amount), $mark)
    }
    $inCount = @($refunds | Where-Object { (ParseDate $_.date) -gt $start }).Count
    Answer "$(Money $total) ($inCount of $($refunds.Count) refunds in window)"
}

# ---------------------------------------------------------------------------
if (Section 'Q07' 'What are our best-selling products?') {
    # mirrors OrderTool.getTopSellingProducts: sums quantity per product over ALL orders (no status filter)
    $asTool = $orders | Group-Object productName | ForEach-Object {
        [pscustomobject]@{
            Product = $_.Name
            Units   = ($_.Group | Measure-Object quantity -Sum).Sum
            Revenue = ($_.Group | ForEach-Object { [decimal]$_.amount * $_.quantity } | Measure-Object -Sum).Sum
        }
    } | Sort-Object Units -Descending
    $completedOnly = $orders | Where-Object { $_.status -eq 'COMPLETED' } | Group-Object productName | ForEach-Object {
        [pscustomobject]@{ Product = $_.Name; Units = ($_.Group | Measure-Object quantity -Sum).Sum }
    } | Sort-Object Units -Descending

    Write-Host '  As the tool computes it (ALL orders, incl. CANCELLED):'
    foreach ($p in $asTool) { Write-Host ("    {0,-22} {1,2} units  revenue {2}" -f $p.Product, $p.Units, (Money $p.Revenue)) }
    Write-Host '  Business-correct (COMPLETED only):'
    foreach ($p in $completedOnly) { Write-Host ("    {0,-22} {1,2} units" -f $p.Product, $p.Units) }

    $cancelled = @($orders | Where-Object { $_.status -eq 'CANCELLED' })
    Quirk "getTopSellingProducts counts CANCELLED orders: $(($cancelled | ForEach-Object { "$($_.id) ($($_.productName), $($_.quantity))" }) -join ', ')"
    Answer "Tool answer: $(($asTool | Select-Object -First 3 | ForEach-Object { "$($_.Product) ($($_.Units))" }) -join ', '). Business answer: $(($completedOnly | Select-Object -First 3 | ForEach-Object { "$($_.Product) ($($_.Units))" }) -join ', ')."
}

# ---------------------------------------------------------------------------
if (Section 'Q08' 'What are the main reasons customers return products?') {
    # mirrors ReturnTool.getReturnsByReason (count per reason, no date filter)
    $byReason = $returns | Group-Object reason | Sort-Object Count -Descending
    foreach ($g in $byReason) { Write-Host ("  {0,-20} {1}   [{2}]" -f $g.Name, $g.Count, (($g.Group.id) -join ', ')) }
    Answer (($byReason | ForEach-Object { "$($_.Name) ($($_.Count))" }) -join ', ')
}

# ---------------------------------------------------------------------------
if (Section 'Q09' 'Customers seem unhappy with the Gaming Mouse Elite - can you investigate?') {
    $p = 'Gaming Mouse Elite'
    $rating   = $ratings | Where-Object { $_.Product -eq $p }
    $pRefunds = @($refunds | Where-Object { $_.productName -eq $p })
    $pReturns = @($returns | Where-Object { $_.productName -eq $p })
    $pReviews = @($reviews | Where-Object { $_.productName -eq $p })
    Write-Host "  Rating: avg $($rating.Avg) over $($rating.Count) review(s) - the catalog's worst (see Q04)"
    Write-Host "  Refunds: $($pRefunds.Count)"
    foreach ($r in $pRefunds) { Write-Host "    $($r.id)  $($r.date)  '$($r.reason)'" }
    Write-Host "  Returns: $($pReturns.Count)"
    Write-Host '  Reviews:'
    foreach ($r in $pReviews) { Write-Host "    $($r.id)  rating $($r.rating): $($r.comment)" }
    Answer "Worst-rated ($($rating.Avg)); $($pRefunds.Count) refunds, both 'Defective product'; reviews cite double-click failure out of the box and side buttons dying after a month; NO returns records."
}

# ---------------------------------------------------------------------------
if (Section 'Q10' 'Should we keep selling the Wireless Headphones? Evidence-based recommendation.') {
    $p = 'Wireless Headphones'
    $rating   = $ratings | Where-Object { $_.Product -eq $p }
    $pRefunds = @($refunds | Where-Object { $_.productName -eq $p })
    $pReturns = @($returns | Where-Object { $_.productName -eq $p })
    $pOrders  = @($orders  | Where-Object { $_.productName -eq $p })
    $units    = ($pOrders | Measure-Object quantity -Sum).Sum
    Write-Host "  Against: $($pRefunds.Count) refunds (most-refunded, see Q02), $($pReturns.Count) returns, avg rating $($rating.Avg)"
    foreach ($r in @($reviews | Where-Object { $_.productName -eq $p -and $_.rating -le 2 })) {
        Write-Host "    $($r.id)  rating $($r.rating): $($r.comment)"
    }
    Write-Host "  For: $units units sold across $($pOrders.Count) orders   [$(($pOrders.id) -join ', ')]"
    Answer "Balanced case citing: $($pRefunds.Count) refunds / $($pReturns.Count) returns / $($rating.Avg) rating vs $units units in $($pOrders.Count) orders. Any cited figure must match these."
}

# ---------------------------------------------------------------------------
if (Section 'Q11' 'Short summary of product quality issues across the catalog.') {
    Write-Host '  Products with objective quality signals (rating <= 2.5, or defect/damage refunds+returns):'
    $defectRefunds = $refunds | Where-Object { $_.reason -eq 'Defective product' } | Group-Object productName
    foreach ($r in ($ratings | Where-Object { $_.Avg -le 2.5 } | Sort-Object Avg)) {
        Write-Host ("    {0,-22} avg {1}" -f $r.Product, $r.Avg)
    }
    foreach ($g in $defectRefunds) { Write-Host ("    {0,-22} {1} 'Defective product' refund(s)  [{2}]" -f $g.Name, $g.Count, (($g.Group.id) -join ', ')) }
    $damaged = @($returns | Where-Object { $_.reason -eq 'Damaged on arrival' })
    Write-Host "    'Damaged on arrival' is the top return reason: $($damaged.Count) of $($returns.Count) returns"
    Answer "Must cover Gaming Mouse Elite (1.5, defect refunds), Wireless Headphones (2.0, most refunded), Coffee Maker Deluxe (2.5, failure review + defect refund); 'Damaged on arrival' as top return reason. Padding with healthy products = failure."
}

# ---------------------------------------------------------------------------
if (Section 'Q12' 'Is there a connection between return reasons and reviews?') {
    Write-Host "  'Wrong size' returns (Winter Jacket):   $((@($returns | Where-Object { $_.reason -eq 'Wrong size' }).id) -join ', ')"
    $rev9 = $reviews | Where-Object { $_.id -eq 'REV-009' }
    Write-Host "  Matching review: REV-009 (Winter Jacket, rating $($rev9.rating)): $($rev9.comment)"
    Write-Host "  Defect/damage returns for Headphones + Coffee Maker match their negative reviews (REV-006/007, REV-011)."
    Answer "Yes - Winter Jacket sizing (returns RET-001/004 vs REV-009) is the clean documented link; defect complaints align for Headphones and Coffee Maker."
}

# ---------------------------------------------------------------------------
if (Section 'Q13' 'How many orders did we get in the last month?') {
    # mirrors OrderTool.getOrdersLastMonth: date strictly after today-1month
    $start = $today.AddMonths(-1)
    $recent = @($orders | Where-Object { (ParseDate $_.date) -gt $start })
    $newest = $orders | Sort-Object { ParseDate $_.date } -Descending | Select-Object -First 1
    Write-Host "  Window: strictly after $($start.ToString('yyyy-MM-dd'))"
    Write-Host "  Newest order in data: $($newest.id) ($($newest.date))"
    if ($recent.Count -eq 0) { Answer 'Zero. Any order the answer mentions is invented.' }
    else { Answer "$($recent.Count) order(s): $(($recent.id) -join ', ') - the data has aged into the window; update questions.json." }
}

# ---------------------------------------------------------------------------
if (Section 'Q14' 'Were there any refunds issued in the last month?') {
    # mirrors RefundTool.getRefundsLastMonth: date strictly after today-1month
    $start = $today.AddMonths(-1)
    $recent = @($refunds | Where-Object { (ParseDate $_.date) -gt $start })
    $newest = $refunds | Sort-Object { ParseDate $_.date } -Descending | Select-Object -First 1
    Write-Host "  Window: strictly after $($start.ToString('yyyy-MM-dd'))"
    Write-Host "  Newest refund in data: $($newest.id) ($($newest.date))"
    if ($recent.Count -eq 0) { Answer 'None. Any refund or amount mentioned is invented.' }
    else { Answer "$($recent.Count) refund(s): $(($recent.id) -join ', ') - the data has aged into the window; update questions.json." }
}

# ---------------------------------------------------------------------------
if (Section 'Q15' 'What happened with order ORD-021?') {
    $o = $orders | Where-Object { $_.id -eq 'ORD-021' }
    $start = $today.AddYears(-1)
    $reachable = (ParseDate $o.date) -gt $start
    Write-Host "  Record: $($o.id)  $($o.productName)  $(Money $o.amount)  qty $($o.quantity)  $($o.date)  $($o.status)"
    Write-Host "  No order-by-id tool exists; only path is getOrdersLastYear (window after $($start.ToString('yyyy-MM-dd')))."
    if ($reachable) { Write-Host '  ORD-021 IS inside that window -> indirectly answerable.' }
    else { Quirk "ORD-021 ($($o.date)) has aged OUT of the last-year window - the question is no longer answerable; update questions.json." }
    Answer "Smart Watch, $(Money $o.amount), $($o.date), status $($o.status) - findable only via the last-year listing."
}

# ---------------------------------------------------------------------------
if (Section 'Q16' 'What happened with order ORD-999?') {
    $o = @($orders | Where-Object { $_.id -eq 'ORD-999' })
    Write-Host "  Orders matching ORD-999: $($o.Count)"
    Answer 'No such order exists. Correct answer says it cannot find it; any product/date/status given is fabricated.'
}

# ---------------------------------------------------------------------------
if (Section 'Q17' 'Generate a 20% discount code for the Winter Jacket?') {
    Write-Host '  Capability check: tools are read-only analytics (orders, returns, refunds, reviews). No discount/write capability.'
    Answer 'Must decline - no discount capability exists. A produced code = invention. Bonus: citing the sizing issue (REV-009) as context.'
}

# ---------------------------------------------------------------------------
if (Section 'Q18' 'What is the weather like in Bucharest today?') {
    Write-Host '  Domain check: all tools are shop analytics. Weather is out of domain.'
    Answer 'Must politely decline and restate scope. Any weather given = invention.'
}

Write-Host ''
Write-Host 'Done. Answers marked QUIRK differ between tool logic and business logic - eval questions should expect the TOOL answer but may reward flagging the discrepancy.' -ForegroundColor Yellow
