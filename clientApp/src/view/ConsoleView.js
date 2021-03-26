import React, { Component } from 'react';
import PropTypes from 'prop-types';

// import cn from 'classnames';

import { tickCount } from '../grid/common.js';
import { Log } from '../util/Logging.js';

// eslint-disable-next-line
import { DataGridScrollBar, _scrollBarWith_ } from '../grid/DataGridScrollBar.js';

import './ConsoleView.scss';



class ConsoleView extends Component {
  static propTypes = {
    width: PropTypes.number,
    height: PropTypes.number,
  }

  constructor (props) {
    super(props);

    this.state = {
      clientWidth: props.width,
      clientHeight: props.height,
      redrawKey: 0
    };

    this._logRID = null;
    this._needToScroll = false;
    this._logDiv = React.createRef();
  }

  componentDidMount() {
    this._logRID = Log.addReceiver(this.onReceiveLog);
  }

  // eslint-disable-next-line
  componentDidUpdate(prevProps, prevState) {
    if( this._needToScroll ) {
      this._needToScroll = false;
      this._logDiv.current.scrollTop = Log.size() * 20;
    }
  }

  componentWillUnmount() {
    Log.removeReceiver(this._logRID);
  }

  static getDerivedStateFromProps(nextProps, prevState) {
    if( prevState.clientWidth !== nextProps.width || prevState.clientHeight !== nextProps.height ) {
      return { clientWidth: nextProps.width, clientHeight: nextProps.height };
    }

    return null;
  }

  // eslint-disable-next-line
  onReceiveLog = (item) => {
    this._needToScroll = true;
    this.setState({ redrawKey: tickCount() });
  }

  handleVScrollChanged = (type, start) => {
    console.log('handleVScrollChanged', type, start);

    this._logDiv.current.scrollTop = start * 22;

    /*
    const { beginRow, rowPerHeight } = this.state;
    let newBegin = beginRow;

    switch( type ) {
      case 'position':
        newBegin = start;
        break;

      case 'pageup':
        newBegin = Math.max(0, beginRow - rowPerHeight + 1);
        break;

      case 'pagedown':
        newBegin = beginRow + rowPerHeight - 1;
        break;

      default:
        break;
    }

    if( newBegin !== beginRow ) {
      this.setBeginRow(newBegin);
    }
    // */
  }

  render() {
    const { clientWidth, clientHeight } = this.state;

    // list of { time(s), text(s), type(n) 0, 1(i), 2, 3, 4 }
    const logList = Log.get();

    const sbWidth = 0; // _scrollBarWith_;
    const visibleCount = Math.ceil((clientHeight - 4) / 22); // (clientHeight - padding) / item height(22)
    const vScroll = false; // logList.length > visibleCount;

    return (
      <div className="consoleBox" style={{ width:clientWidth, height:clientHeight }}>
        <div ref={this._logDiv} className="consoleLogDiv" style={{ width:(clientWidth - sbWidth), height:clientHeight }}>
          { logList.map((o, i) => (<div key={`${o.time}-${i}`} className={`consoleItem consoleItem${o.type}`}>{ `[${o.time}] ${o.text}` }</div>)) }
        </div>
        { vScroll &&
          <DataGridScrollBar
            barHeight={clientHeight}
            barWidth={sbWidth}
            vertical={true}
            onPositionChanged={this.handleVScrollChanged}
            initialPosition={Math.max(logList.length - visibleCount, 0)}
            visibleCount={visibleCount}
            total={logList.length}
          />
        }
      </div>
    );
  }
}

export default ConsoleView;
export { ConsoleView };
