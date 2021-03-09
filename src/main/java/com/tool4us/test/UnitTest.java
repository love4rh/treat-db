package com.tool4us.test;

import com.tool4us.common.YMLParser;



/**
 * 모듈 단위 테스트를 위한 클래스.
 * 
 * @author TurboK
 */
public class UnitTest
{
    public static void main(String[] args)
    {
        testYMLParser();
    }

    public static void testYMLParser()
    {
        final String newLine = System.getProperty("line.separator");
        
        String[] testYml = new String[]
        {
              "---" + newLine
            + "vroot: 'D:\\EclipseData\\Andromeda\\treatDB\\clientApp\\build'" + newLine

            + "---" + newLine
            + "map1: # 코멘트" + newLine
            + "# 라인 코멘트" + newLine
            + "  child0:    " + newLine
            + "  child1: ab#c" + newLine
            + "  child2: e-fc" + newLine
            + "test0:" + newLine
            + "test1:" + newLine
            + "  id: 123" + newLine
            + "  name: hong" + newLine
            + "  desc: " + newLine
            + "  phone: 123" + newLine
            + "test2:" + newLine
            + "  - aaa" + newLine
            + "    - dfsa" + newLine
            + "  - id: 123" + newLine
            + "    name: hong" + newLine
            + "test3:" + newLine
            + " - a1" + newLine
            + " - a2" + newLine
            + "test4:" + newLine
            + "- - - a - -" + newLine
            + "      - b" + newLine
            + "  - " + newLine
            + "test5: |" + newLine
            + "  abc" + newLine
            + "  def" + newLine
            + "  " + newLine
            + "test6:" + newLine
            + "  - > " + newLine
            + "   ghijk" + newLine
            + "   lmnopqr sudr" + newLine
            + "  - ddd" + newLine
            + "quotest1: \\abf''" + newLine
            + "quotest2: 'abc # 코멘트" + newLine
            + "eef" + newLine
            + "'" + newLine
            + "quotest3:" + newLine
            + "- I'm the best!" + newLine
            + "- 'ghj' # Comment 2" + newLine
            + "quotest4:" + newLine
            
            , "---" + newLine
            + "# this is comment." + newLine
            + "name: kim" + newLine
            + "job: designer" + newLine
            + "married: true" + newLine
            + "age: 20" + newLine
            + "" + newLine
            + "fruit_list:" + newLine
            + "- apple" + newLine
            + "- banana" + newLine
            + "" + newLine
            + "object:" + newLine
            + "  name: kim" + newLine
            + "  job: developer" + newLine
            + "" + newLine
            + "object_list:" + newLine
            + "- color: red" + newLine
            + "  direction: left" + newLine
            + "- color: blue" + newLine
            + "  direction: right" + newLine
            + "comment_multi_line: |" + newLine
            + "  Hello world." + newLine
            + "  This is Kim." + newLine
            + "  " + newLine
            + "comment_single_line: >" + newLine
            + "  Hello world." + newLine
            + "  This is Kim." + newLine
           
            , "---" + newLine 
            + ">" + newLine
            + "  abc" + newLine
            + "  def" + newLine
            
            , "---\n- Mark McGwire\n- Sammy Sosa\n- Ken Griffey\n"
            , "---\nhr:  65    # Home runs\navg: 0.278 # Batting average\nrbi: 147   # Runs Batted In\n"
            
            , "---" + newLine
            + "american:" + newLine
            + "  - Boston Red Sox" + newLine
            + "  - Detroit Tigers" + newLine
            + "  - New York Yankees" + newLine
            + "national:" + newLine
            + "  - New York Mets" + newLine
            + "  - Chicago Cubs" + newLine
            + "  - Atlanta Braves" + newLine
            
            , "---" + newLine
            + "-" + newLine
            + "  name: Mark McGwire" + newLine
            + "  hr:   65" + newLine
            + "  avg:  0.278" + newLine
            + "-" + newLine
            + "  name: Sammy Sosa" + newLine
            + "  hr:   63" + newLine
            + "  avg:  0.288" + newLine

            // 에러가 발생해야 정상임.
            , "---" + newLine
            + "- a" + newLine
            + "- b" + newLine
            + "- abc" + newLine
            + "dfd" + newLine
        };
        
        for(String ymlText : testYml)
        {
            try
            {
                System.out.println("---");
                Object result = YMLParser.toJsonObject(ymlText, 0);
                System.out.println(result);
            }
            catch(Exception xe)
            {
                xe.printStackTrace();
            }
        }
    }
}
