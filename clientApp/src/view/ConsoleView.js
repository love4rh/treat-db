import React, { Component } from 'react';
import PropTypes from 'prop-types';

// import cn from 'classnames';

import { tickCount } from '../grid/common.js';

import { Log } from '../util/Logging.js';

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
      redrawKey: tickCount()
    };

    this._logRID = null;
  }

  componentDidMount() {
    this._logRID = Log.addReceiver(this.onReceiveLog);
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

  onReceiveLog = () => {
    this.setState({ redrawKey: tickCount() });
  }

  render() {
    const { clientWidth, clientHeight } = this.state;

    // list of { time(s), text(s), type(n) 0, 1(i), 2, 3, 4 }
    const logList = Log.get();

    return (
      <div className="consoleBox" style={{ width:clientWidth, height:clientHeight }}>
        { logList.map((o, i) => (<div key={`${o.time}-${i}`} className={`consoleItem${o.type}`}>{ `[${o.time}] ${o.text}` }</div>)) }
      </div>
    );
  }
}

export default ConsoleView;
export { ConsoleView };
