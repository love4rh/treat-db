import React, { Component } from 'react';

import cn from 'classnames';

import { LayoutDivider, DividerDirection } from '../component/LayoutDivider.js';

import QuerySpace from '../view/QuerySpace.js';

import './SQLFrame.scss';



class SQLFrame extends Component {
  constructor (props) {
    super(props);

    this.state = {
      clientWidth: 600,
      clientHeight: 400,
      bottomHeight: 150,
      leftWidth: 200
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

    // console.log('SQLFrame onResize', clientWidth, clientHeight);
    this.setState({ clientWidth, clientHeight });
  }

  handleLayoutChanged = (type) => (from, to) => {
    const { bottomHeight, leftWidth } = this.state;

    if( 'topBottom' === type ) {
      console.log('layout top-bottom', bottomHeight, to - from);
      this.setState({ bottomHeight: bottomHeight + to - from });
    } else if( 'leftRight' === type ) {
      console.log('layout left-right', leftWidth, to - from);
      this.setState({ leftWidth: leftWidth + to - from });
    }
  }

  render() {
    const dividerSize = 4;
    const { clientWidth, clientHeight, bottomHeight, leftWidth } = this.state;

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
          <div className="leftPane" style={{ flexBasis:`${clientWidth - leftWidth - dividerSize}px` }}>
            <QuerySpace width={clientWidth - leftWidth - dividerSize} height={clientHeight - bottomHeight - dividerSize} />
          </div>
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
