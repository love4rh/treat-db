import React, { Component } from 'react';
import PropTypes from 'prop-types';

import cn from 'classnames';

import { isvalid, isundef, nvl, numberWithCommas } from './common.js';
import { VscCaseSensitive, VscArrowDown, VscArrowUp, VscClose } from 'react-icons/vsc';

import './styles.scss';



class FindDialog extends Component {
  static propTypes = {
    doHoldEvent: PropTypes.func,
  	top: PropTypes.number,
  	right: PropTypes.number,
  	prevResult: PropTypes.object,
    onEvent: PropTypes.func,
  };

  constructor (props) {
    super(props);

    const { result, ridx, keyword, caseSensitive } = this.props.prevResult;

    this.state = {
    	sKeyword: nvl(keyword, ''),
    	caseSensitive: nvl(caseSensitive, false),
    	foundTotal: isvalid(result) ? result.length : 0,
    	currentPos: nvl(ridx, -1),
    	result: isvalid(result) ? result : {}
    };
  }

  static getDerivedStateFromProps (nextProps, prevState) {
    const { result, ridx } = nextProps.prevResult;

    // 새로운 검색을 해야 하는 경우임
    if( isundef(result) ) {
      return {
        foundTotal: isvalid(result) ? result.length : 0,
        currentPos: nvl(ridx, -1),
        result: isvalid(result) ? result : {}
      };
    } else if( ridx !== prevState.currentPos ) {
      return { currentPos: nvl(nextProps.prevResult.ridx, -1) };
    }

    return null;
  }

  procFind = (value, caseSensitive) => () => {
  	value = value.trim();
  	if( value === '' ) {
  		return;
  	}

  	const { onEvent } = this.props;

  	if( onEvent ) {
  		onEvent('find', { keyword:value, caseSensitive }, (res, cidx, ridx) => {
  			this.setState({
  				foundTotal: res.length,
  				currentPos: nvl(ridx, -1),
  				result: { res, keyword:value, caseSensitive }
  			});
  		});
  	}
  }

  handleChangeKeyword = (ev) => {
  	this.setState({ sKeyword:ev.target.value });
  }

  handleClickToolButton = () => {
  	const { sKeyword, caseSensitive } = this.state;

  	const c = !caseSensitive;

  	this.setState({ caseSensitive:!caseSensitive });
  	this.procFind(sKeyword, c)();
  }

  handleFind = (type) => () => {
  	const { onEvent } = this.props;

  	if( !onEvent ) {
  		return;
  	}

  	const { sKeyword, caseSensitive, result } = this.state;

    if( isundef(result.res) ) {
      this.procFind(sKeyword, caseSensitive)();
    }

  	onEvent(type, { keyword:sKeyword.trim, caseSensitive });
  }

  handleClose = () => {
  	const { onEvent } = this.props;

  	if( onEvent ) {
  		onEvent('close');
  	}
  }

  handleKeyDown = (ev) => {
    // console.log('keydown', ev.keyCode, ev.key, ev.ctrlKey, ev.altKey, ev.shiftKey, ev.repeat);
    let processed = true;

    switch( ev.keyCode ) {
      case 27: // Escape
      	this.handleClose();
        break;

      case 13: { // Enter
      	const { sKeyword, caseSensitive, result } = this.state;

      	if( result.keyword === sKeyword && result.caseSensitive === caseSensitive ) {
      		this.handleFind(ev.shiftKey ? 'prev' : 'next')();
      	} else {
      		this.procFind(sKeyword, caseSensitive)();
      	}
      } break;

      default:
      	processed = false;
      	break;
    }

    if( processed && ev.preventDefault ) {
      ev.preventDefault();
      ev.stopPropagation();
    }
  }

  holdGridEvent = (set) => () => {
    this.props.doHoldEvent(set);
  }

  render () {
  	const { top, right } = this.props;
  	const { sKeyword, caseSensitive, foundTotal, currentPos } = this.state;
  	const w = 160 + 26 * 1 + 110 + 28 * 3 + 10;

  	return (
  		<div className="findDialog"
  			style={{ top:(top - 1), left:(right - w - 2), width:w }}
  			onKeyDown={this.handleKeyDown}
  		>
  			<div className="findMainElem">
	  			<input
	  				type="text"
	  				className="findInputBox"
	          placeholder={'Find...'}
	          value={sKeyword}
	          autoFocus={true}
	          onChange={this.handleChangeKeyword}
            onFocus={this.holdGridEvent(true)}
            onBlur={this.holdGridEvent(false)}
	        />
	        <div
	        	className={cn({ 'findInputButton': true, 'findButtonPressed': caseSensitive })}
	        	onClick={this.handleClickToolButton}
	        >
		      	<VscCaseSensitive size="16" />
		      </div>
		    </div>
		    <div className="findResultBox">
		    	{numberWithCommas(currentPos + 1) + ' of ' + numberWithCommas(foundTotal)}
		    </div>
		    <div className={cn({ 'findNavitButton': true })} onClick={this.handleFind('prev')}>
	      	<VscArrowUp size="16" />
	     	</div>
	     	<div className={cn({ 'findNavitButton': true })} onClick={this.handleFind('next')}>
	      	<VscArrowDown size="16" />
	     	</div>
	     	<div className={cn({ 'findNavitButton': true })} onClick={this.handleClose}>
	     		<VscClose size="16" />
	     	</div>
  		</div>
  	);
  }
}

export default FindDialog;
export { FindDialog };
