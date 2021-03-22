import React, { Component } from 'react';
import PropTypes from 'prop-types';

// import cn from 'classnames';

import loader from "@monaco-editor/loader";

import { LayoutDivider, DividerDirection } from '../component/LayoutDivider.js';

import DiosDataSource from '../grid/DiosDataSource.js';
import DataSource from '../grid/DataSource.js';
import DataGrid from '../grid/DataGrid.js';

import './QuerySpace.scss';



class QuerySpace extends Component {
  static propTypes = {
    width: PropTypes.number,
    height: PropTypes.number,
  }

  constructor (props) {
    super(props);

    const ds = new DataSource({ columnCount: 20, rowCount: 1000 });

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

      const properties = {
        value: 'SELECT *\n  FROM TB_TABLE\n WHERE COL1 = 1\n\nSELECT *\n  FROM TB_TABLE\n WHERE COL2 = 1\n\n',
        language: 'sql',
        automaticLayout: true,
        roundedSelection: false,
	      scrollBeyondLastLine: false,
        theme: 'vs-dark'
      }

      this._editor = monaco.editor.create(wrapper, properties);
      console.log(this._editor.getRawOptions());
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

  handleLayoutChanged = (from, to) => {
    const { gridHeight } = this.state;
    this.setState({ gridHeight: gridHeight + to - from });
  }

  render() {
    const dividerSize = 4;
    const { clientWidth, clientHeight, gridHeight, ds } = this.state;
    const editorHeight = clientHeight - gridHeight - dividerSize;

    return (
      <div className="queryBox">
        <div className="editorPane" style={{ flexBasis:`${editorHeight}px` }}>
          <div id="monacoSqlEditor" style={{ height: editorHeight, width: clientWidth }} />
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
