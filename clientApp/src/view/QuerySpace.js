import React, { Component } from 'react';
import PropTypes from 'prop-types';

import { isundef, nvl, makeOneLine, tickCount } from '../grid/common.js';

import loader from "@monaco-editor/loader";
import { apiProxy } from '../util/apiProxy.js';

import { LayoutDivider, DividerDirection } from '../component/LayoutDivider.js';

import DataGrid from '../grid/DataGrid.js';
import DiosDataSource from '../grid/DiosDataSource.js';

import GuideDataSource from '../data/GuideDataSource.js';

import { Log } from '../util/Logging.js';
import { AppData } from '../data/AppData.js';

import sampleData from '../mock/data.json';

import './QuerySpace.scss';



class QuerySpace extends Component {
  static propTypes = {
    width: PropTypes.number,
    height: PropTypes.number,
  }

  constructor (props) {
    super(props);

    const ds = new GuideDataSource();

    this.state = {
      gridHeight: 350,
      clientWidth: props.width,
      clientHeight: props.height,
      ds
    };

    this._editor = null;
  }

  componentDidMount() {
    loader.init().then(monaco => {
      const wrapper = document.getElementById('monacoSqlEditor');
      const textValue = localStorage.getItem('latestQuery');

      const properties = {
        value: nvl(textValue, '\n'),
        language: 'sql',
        automaticLayout: true,
        roundedSelection: false,
	      scrollBeyondLastLine: false,
        theme: 'vs-dark'
      }

      this._editor = monaco.editor.create(wrapper, properties);
    })
    .catch(xe => {
      console.log('loader editor', xe);
    });
  }

  componentWillUnmount() {
    //
  }

  static getDerivedStateFromProps(nextProps, prevState) {
    if( prevState.clientWidth !== nextProps.width || prevState.clientHeight !== nextProps.height ) {
      return { clientWidth: nextProps.width, clientHeight: nextProps.height };
    }

    return null;
  }

  extractQueryBlock = (text, pos) => {
    if( isundef(text) || isundef(pos) ) {
      return null;
    }

    let { lineNumber } = pos;
    const lines = text.split('\n');

    if( isundef(lineNumber) || lineNumber < 1 || lineNumber > lines.length ) {
      return null;
    }

    lineNumber -= 1; // 0-based;

    let s = lineNumber - 1;
    while( s >= 0 ) {
      const str = lines[s].trim();
      if( str === '' || str.endsWith(';') ) {
        break;
      }
      s -= 1;
    }
    s += 1;

    let e = lineNumber;
    while( e < lines.length ) {
      const str = lines[e].trim();
      if( str === '' ) {
        break;
      } else if( str.endsWith(';') ) {
        e += 1;
        break;
      }
      e += 1;
    }

    return lines.slice(s, e).join('\n');
  }

  handleLayoutChanged = (from, to) => {
    const { gridHeight } = this.state;
    this.setState({ gridHeight: gridHeight + to - from });
  }

  setDataSource = (data) => {
    this.setState({ ds: new DiosDataSource(data) });
  }

  handleEditorKeyDown = (ev) => {
    // console.log('handleEditorKeyDown', ev.keyCode, ev.key, ev.ctrlKey, ev.altKey, ev.shiftKey, ev.repeat);

    // shift + enter --> 쿼리 실행
    if( ev.shiftKey && ev.keyCode === 13 ) {
      ev.preventDefault();
      ev.stopPropagation();
      const textValue = this._editor.getValue();
      const query = this.extractQueryBlock(textValue, this._editor.getPosition());

      localStorage.setItem('latestQuery', textValue);

      if( isundef(query) || query.trim() === '' ) {
        Log.w('invalid query statements');
      } else {
        const sTick = tickCount();
        Log.n('executing [' + makeOneLine(query) + ']');

        this.setDataSource(sampleData);

        /*
        apiProxy.executeQuery(AppData.getDatabase(), query,
          (res) => {
            console.log('query execute', res);
            Log.i('query executed and first query result received [' + (tickCount() - sTick) + 'ms]');
            // res.response.qid
            this.setDataSource(res.response);
            
          },
          (err) => {
            console.log('query error', err);
            if( isundef(err.data.returnMessage) ) {
              Log.w('error occurrs in executing the query.');
            } else {
              Log.w(err.data.returnMessage);
            }
          }
        ); // */
      }
    }
  }

  render() {
    const dividerSize = 4;
    const { clientWidth, clientHeight, gridHeight, ds } = this.state;
    const editorHeight = clientHeight - gridHeight - dividerSize;

    return (
      <div className="queryBox">
        <div className="editorPane" style={{ flexBasis:`${editorHeight}px` }}>
          <div
            id="monacoSqlEditor"
            style={{ height: editorHeight, width: clientWidth }}
            onKeyDown={this.handleEditorKeyDown}
          />
        </div>
        <LayoutDivider direction={DividerDirection.horizontal}
          size={dividerSize}
          onLayoutChange={this.handleLayoutChanged}
        />
        <div className="resultPane" style={{ flexBasis:`${gridHeight}px`, height: gridHeight, width: clientWidth }}>
          <DataGrid
            width={clientWidth}
            dataSource={ds}
            userBeginRow={0}
          />
        </div>
      </div>
    );
  }
}

export default QuerySpace;
