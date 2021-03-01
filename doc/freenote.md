/**
 * 0: 아직 모름
 * 1: 단순 값
 * 2: 목록 (-)
 * 3: 멀티라인 문자열 (|)
 * 5: 멀티라인 문자열 (멀티 싱글라인 >) 
 * 4: 객체
 */

 
https://www.convertjson.com/yaml-to-json.htm
 
https://yaml-online-parser.appspot.com/

``` yaml
---
p1:
  p2:
    p3: |
      asf
      ss
    p4: sdf #sdfksfj # sdf

test: >
  sdfj
  sdflsj

list:
- { a: ss, b: dfd }
- >
    id: 1
    name: Johnson, Smith, and Jones Co.
    amount: 345.33
    Remark: Pays on time
-
    id: 2
    name: Sam "Mad Dog" Smith
    amount: 993.44
    Remark:
-
    id: 3
    name: Barney & Company
    amount: 0
    Remark: "Great to work with\nand always pays with cash."
-
    id: 4
    name: 'Johnson''s Automotive'
    amount: 2344
    Remark: 
```