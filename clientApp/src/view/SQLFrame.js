import React, { Component } from 'react';

import cn from 'classnames';

import { LayoutDivider, DividerDirection } from '../component/LayoutDivider.js';

import MetaViewer from '../view/MetaViewer.js';
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

    const mainWidth = clientWidth - leftWidth - dividerSize;
    const mainHeight = clientHeight - bottomHeight - dividerSize;

    return (
      <div ref={this._mainDiv} className="sqlFrame">
        <div className="topPane">
          <div className="leftPane" style={{ flexBasis:`${leftWidth}px` }}>
            <MetaViewer width={leftWidth} height={mainHeight} />
          </div>
          <LayoutDivider direction={DividerDirection.vertical}
            size={dividerSize}
            onLayoutChange={this.handleLayoutChanged('leftRight')}
          />
          <div className="rightPane" style={{ flexBasis:`${mainWidth}px` }}>
            <QuerySpace width={mainWidth} height={mainHeight} />
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
