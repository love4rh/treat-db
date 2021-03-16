import React, { Component } from 'react';

import cn from 'classnames';

import { LayoutDivider, DividerDirection } from '../component/LayoutDivider.js';

import QuerySpace from '../view/QuerySpace.js';

import './SQLFrame.scss';



class SQLFrame extends Component {
  constructor (props) {
    super(props);

    this.state = {
      clientWidth: 800,
      clientHeight: 600,
      bottomHeight: 150,
      leftWidth: 300
    };

    this._mainDiv = React.createRef();
  }

  componentDidMount() {
    this.onResize();
    window.addEventListener('resize', this.onResize);
  }

  componentWillUnmount() {
    window.removeEventListener('resize', this.onResize);
  }

  onResize = () => {
    const { clientWidth, clientHeight } = this._mainDiv.current;

    console.log('SQLFrame onResize', clientWidth, clientHeight);

    this.setState({ clientWidth, clientHeight });
  }

  handleLayoutChanged = (type) => (from, to) => {
    const { bottomHeight, leftWidth } = this.state;

    if( 'topBottom' === type ) {
      this.setState({ bottomHeight: bottomHeight + to - from });
    } else if( 'leftRight' === type ) {
      this.setState({ leftWidth: leftWidth + to - from });
    }
  }

  render() {
    const { clientWidth, clientHeight, bottomHeight, leftWidth } = this.state;
    const dividerSize = 4;

    return (
      <div ref={this._mainDiv} className="sqlFrame">
        <div className="topPane">
          <div className="leftPane" style={{ flexBasis:`${leftWidth}px` }}>
            Left Pane
          </div>
          <LayoutDivider direction={DividerDirection.vertical}
            size={dividerSize}
            onLayoutChange={this.handleLayoutChanged('leftRight')}
          />
          <QuerySpace width={clientWidth - leftWidth - dividerSize} height={clientHeight - bottomHeight - dividerSize} />
        </div>
        <LayoutDivider direction={DividerDirection.horizontal}
          size={dividerSize}
          onLayoutChange={this.handleLayoutChanged('topBottom')}
        />
        <div className="bottomPane" style={{ flexBasis:`${bottomHeight}px` }}>
          Console
        </div>
      </div>
    );
  }
}

export default SQLFrame;
