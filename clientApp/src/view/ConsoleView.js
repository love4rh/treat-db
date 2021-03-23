import React, { Component } from 'react';
import PropTypes from 'prop-types';

// import cn from 'classnames';

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
    };
  }

  componentDidMount() {
    //
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

  render() {
    const { clientWidth, clientHeight } = this.state;

    return (
      <div className="consoleBox">
        Console
      </div>
    );
  }
}

export default ConsoleView;
export { ConsoleView };
