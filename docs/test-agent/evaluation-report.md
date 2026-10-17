# Reward test defect detection evaluation

Control passed: true
Selected injected defects detected: 5/5
Original service unchanged: true
Test SHA256: 2447d6bb856dc7433c2a89526d75daeaf0affe6c1cfe2d629eb877e64105f5b3

| Case | Classification | Tests | Failures | Detected by assertion |
| --- | --- | --- | --- | --- |
| control | passed | 20 | 0 | control |
| accept-nonpositive-points | test_failure | 20 | 3 | true |
| duplicate-collection | test_failure | 20 | 1 | true |
| wrong-default-tier | test_failure | 20 | 1 | true |
| lost-point-origin | test_failure | 20 | 1 | true |
| wrong-contact-axis | test_failure | 20 | 1 | true |

These are five selected mutations, not broad mutation coverage or proof of production correctness. Compilation/environment failures do not count as detections. Mockito assertion failures count; incidental exceptions alone do not. Each case runs in a separate copy; production sources are not edited.
