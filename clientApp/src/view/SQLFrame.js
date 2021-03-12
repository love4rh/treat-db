import React, { Component } from 'react';

import cn from 'classnames';

import { LayoutDivider, DividerDirection } from '../component/LayoutDivider.js';

import './SQLFrame.scss';



class SQLFrame extends Component {
  constructor (props) {
    super(props);

    this.state = {
      bottomHeight: 250
    };
  }

  componentDidMount() {
    //
  }

  componentWillUnmount() {
    //
  }

  handleLayoutChange = (from, to) => {
    const { bottomHeight } = this.state;
    this.setState({ bottomHeight: bottomHeight + to - from });
  }

  render() {
    const { bottomHeight } = this.state;

    return (
      <div className="sqlFrame">
        <div
          className="topPane"
        >
          Workspace
        </div>
        <LayoutDivider direction={DividerDirection.horizontal}
          size={4}
          onLayoutChange={this.handleLayoutChange}
        />
        <div
          className="bottomPane"
          style={{ flexBasis:`${bottomHeight}px` }}
        >
          Console
        </div>
      </div>
    );
  }
}

export default SQLFrame;
