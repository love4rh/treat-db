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
map1: # 코멘트
# 라인 코멘트
  child0:    
  child1: ab#c
  child2: e-fc
test0:
test1:
  id: 123
  name: hong
  desc: 
  phone: 123
test2:
  - aaa
    - dfsa
  - id: 123
    name: hong
test3:
 - a1
 - a2
test4:
- - - a - -
      - b
  - 
test5: |
  abc
  def
  
test6:
  - > 
   ghijk
   lmnopqr sudr
  - ddd
quotest1: \\abf''
quotest2: 'abc
eef
'
quotest3:
- I'm the best!
- 'ghj'
quotest4:
```