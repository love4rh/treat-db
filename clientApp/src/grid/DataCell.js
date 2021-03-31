import React, { Component } from 'react';
import PropTypes from 'prop-types';

import cn from 'classnames';

import { isvalid, numberWithCommas } from './common.js';

import './styles.scss';



class DataCell extends Component {
  static propTypes = {
    type: PropTypes.string,
    selected: PropTypes.bool,
    width: PropTypes.number,
    height: PropTypes.number, // rowHeight
    lineHeight: PropTypes.string,
    doHoldEvent: PropTypes.func,
    col: PropTypes.number,
    row: PropTypes.number,
    fixedType: PropTypes.number // 0: normal, 1: fixed, 2: last fixed
  }

  constructor (props) {
    super(props);

    const { value, fixedType } = this.props;
    const isObject = typeof value === 'object';

    this.state = {
      editable: 0, // 0: 에디트 불가, 1: 에디트 가능, 2: 에디팅
      value: isvalid(value) ? '' + value : '',
      isObject,
      fixedType
    };
  }

  static getDerivedStateFromProps (nextProps, prevState) {
    const ns = {};
    let assigned = false;

    if( !nextProps.selected && prevState.editable !== 0 ) {
      if( nextProps.doHoldEvent ) {
        nextProps.doHoldEvent(false, nextProps.col, nextProps.row);
      }
      ns.editable = 0;
      assigned = true;
    }

    const nv = isvalid(nextProps.value) ? '' + nextProps.value : '';
    if( nv !== prevState.value ) {
      ns.value = nv;
      assigned = true;
    }

    if( nextProps.fixedType !== prevState.fixedType ) {
      ns.fixedType = nextProps.fixedType;
      assigned = true;
    }

    return assigned ? ns : null;
  }

  handleClick = (ev) => {
    const { selected, col, row } = this.props;
    const { editable, isObject } = this.state;

    if( editable === 1 && !isObject ) {
      ev.preventDefault();
      ev.stopPropagation();

      if( this.props.doHoldEvent ) {
        this.props.doHoldEvent(true, col, row);
      }

      this.setState({ editable: 2 });
    } else if( selected ) {
      ev.preventDefault();
      ev.stopPropagation();

      this.setState({ editable: 1 });
    }
  }

  handleKeyDown = (ev) => {
    const { keyCode } = ev;

    if( keyCode === 27 ) { // escape
      this.handleBlur();
    } else if( keyCode === 13 ) { // enter
      // TODO do something?
      this.handleBlur();
    }
  }

  handleChanged = (ev) => {
    // this.setState({ value: ev.target.value }); // read-only
  }

  handleBlur = () => {
    this.setState({ editable: 0 });

    if( this.props.doHoldEvent ) {
      this.props.doHoldEvent(false, this.props.col, this.props.row);
    }
  }

  render () {
    const { type, selected, width, height, lineHeight, children } = this.props;
    const { value, editable, fixedType, isObject } = this.state;

    return (
      <React.Fragment>
        { editable === 2 ?
          <input type="text"
            className="dataCellEditor"
            value={'' + value}
            autoFocus={true}
            style={{ width:width-2, height:height-2 }}
            onChange={this.handleChanged}
            onKeyDown={this.handleKeyDown}
            onBlur={this.handleBlur}
          /> :
          <div
            className={cn({ 'dataCell':true, 'selectedCell':selected, 'alignRight':type === 'number', 'fixedLine': fixedType === 2 })}
            style={{ width, height, lineHeight, flexBasis:width }}
            onClick={this.handleClick}
          >
            { isObject ? children : ('number' === type ? numberWithCommas(value) : value) }
          </div>
        }
      </React.Fragment>
    );
  }
}


export default DataCell;
export { DataCell };
