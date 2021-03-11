import React, { Component } from 'react';

import cn from 'classnames';

import Editor from "@monaco-editor/react";

import './EditorFrame.scss';



class EditorFrame extends Component {
  constructor (props) {
    super(props);

    this._refEditor = React.createRef();
  }

  componentDidMount() {
    //
  }

  componentWillUnmount() {
    //
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
    //

    return (
      <div className="editorFrame">
        <Editor
          className="editorBox"
          defaultLanguage="sql"
          defaultValue="// some comment"
          theme="vs-dark"
          beforeMount={this.handleEditorWillMount}
          onMount={this.handleEditorDidMount}
        />
      </div>
    );
  }
}

export default EditorFrame;
