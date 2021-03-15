import React, { Component } from 'react';

import cn from 'classnames';

import Editor from "@monaco-editor/react";

import { LayoutDivider, DividerDirection } from '../component/LayoutDivider.js';

import './QuerySpace.scss';



class QuerySpace extends Component {
  constructor (props) {
    super(props);

    this.state = {
      gridHeight: 250,
      clientHeight: 500
    };

    this._mainBox = React.createRef();
    this._refEditor = React.createRef();
  }

  componentDidMount() {
    this.setState({ clientHeight: this._mainBox.current.clientHeight });
  }

  componentWillUnmount() {
    //
  }

  handleLayoutChanged = (from, to) => {
    const { gridHeight } = this.state;
    this.setState({ gridHeight: gridHeight + to - from, clientHeight: this._mainBox.current.clientHeight });
  }

  handleEditorWillMount = (monaco) => {
    // here is the monaco instance
    // do something before editor is mounted
    monaco.languages.typescript.javascriptDefaults.setEagerModelSync(true);
  }

  handleEditorDidMount = (editor, monaco) => {
    // here is another way to get monaco instance
    // you can also store it in `useRef` for further usage
    this._refEditor.current = editor; 
  }

  render() {
    const { clientHeight, gridHeight } = this.state;
    const dividerSize = 4;
    const editorHeight = clientHeight - gridHeight;

    return (
      <div ref={this._mainBox} className="queryBox">
        <div className="editorPane" style={{ flexBasis:`${editorHeight}px` }}>
          <Editor
            height={editorHeight}
            defaultLanguage="sql"
            defaultValue="SELECT * FROM TB_N_SVC_BAS"
            theme="vs-dark"
            beforeMount={this.handleEditorWillMount}
            onMount={this.handleEditorDidMount}
          />
        </div>
        <LayoutDivider direction={DividerDirection.horizontal}
          size={dividerSize}
          onLayoutChange={this.handleLayoutChanged}
        />
        <div className="resultPane">
          GRID
        </div>
      </div>
    );
  }
}

export default QuerySpace;
