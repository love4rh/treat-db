import React, { Component } from 'react';
import PropTypes from 'prop-types';

import { isvalid, makeid, istrue } from './common.js';
import { RiCheckboxBlankLine, RiCheckboxLine, RiCheckboxIndeterminateLine, RiCheckboxBlankFill, RiCheckLine, RiCloseLine } from "react-icons/ri";

import { DataGridScrollBar, _scrollBarWith_ } from './DataGridScrollBar.js';

import './styles.scss';


const _lh_ = 26; // 아이템 라인 높이
const _visCount_ = 9; // 한번에 표시할 최대 아이템 크기



class FullCheckBox extends Component {
  static propTypes = {
    title: PropTypes.string.isRequired,
    checked: PropTypes.bool,
    intermediated: PropTypes.bool,
    color: PropTypes.string,
    onChanged: PropTypes.func
  };

  constructor (props) {
    super(props);

    const { title, checked, color, intermediated } = props;

    this.state = {
      title: title,
      checked: istrue(checked),
      intermediated,
      color: color
    }
  }

  handleClick = () => {
    const { onChanged } = this.props;
    const { checked } = this.state;

    this.setState({ checked:!checked, intermediated:false })

    if( onChanged ) {
      onChanged(!checked);
    }
  }

  render () {
    const { title, checked, color, intermediated } = this.state;

    const chkTag = (intermediated && <RiCheckboxIndeterminateLine size="16" />)
      || (checked ? <RiCheckboxLine size="16" /> : <RiCheckboxBlankLine size="16" />);

    return (
      <div className="chkBox" style={{ height:_lh_ }} onClick={this.handleClick}>
        <div className="chkCheckBox">{chkTag}</div>
        <div className="chkTitle">{title}</div>
        { isvalid(color) && <div className="chkColorBox"><RiCheckboxBlankFill size="16" color={`#${color}`} /></div> }
      </div>
    );
  }
}



class ColumnFilter extends Component {
  static propTypes = {
    top: PropTypes.number,
    right: PropTypes.number,
    data: PropTypes.array,
    onDone: PropTypes.func
  };

  constructor (props) {
    super(props);

    const original = JSON.parse(JSON.stringify(props.data));

    this.state = {
      original,
      sKeyword: '',
      compValue: '',
      drawKey: makeid(6),
      beginIdx: 0,
      attaching: false,
      items: original,
      selectedCount: original.reduce((r, o) => r + (o.selected ? 1 : 0), 0),
      idxList: null
    };
  }

  procKeyword = (value) => {
    const comp = value.trim();
    const { compValue, original } = this.state;

    if( compValue === comp ) {
      return;
    }

    let newItems = original;
    let idxList = null;

    if( comp !== '' ) {
      idxList = [];
      newItems = original.filter((o, idx) => {
        if( o.title.indexOf(comp) !== -1 ) {
          idxList.push(idx);
          return true;
        }
        return false;
      });
    }

    this.setState({
      sKeyword: value,
      compValue: comp,
      drawKey: makeid(6),
      beginIdx: 0,
      attaching: false,
      items: newItems,
      selectedCount: newItems.reduce((r, o) => r + (o.selected ? 1 : 0), 0),
      idxList
    });
  }

  handleChangeKeyword = (ev) => {
    this.procKeyword(ev.target.value);
  }

  handleClear = () => {
    this.procKeyword('');
  }

  handleFilterChanged = (idx) => (checked) => {
    const { items, selectedCount } = this.state;

    // console.log('check click', idx, checked);

    let selCount = 0;
    if( idx === -1 ) { // select all
      for(let i = 0; i < items.length; ++i) {
        items[i].selected = checked;
      }
      selCount = checked ? items.length : 0;
    } else if( idx === -2 ) { // attach filtered
      this.setState({ attaching:checked });
      selCount = selectedCount
    } else {
      items[idx].selected = checked;
      selCount = selectedCount + (checked ? 1 : -1);
    }

    this.setState({ drawKey: makeid(6), selectedCount: selCount });
  }

  // eslint-nex-line-disabled
  handleVScrollChanged = (type, start) => {
    this.setState({ beginIdx: Math.floor(start) });
  }

  handleValueWheel = (ev) => {
    ev.stopPropagation();
    this.moveSelectPosition( (ev.deltaY < 0 ? -1 : 1) * Math.ceil(Math.abs(ev.deltaY) / 100) );
  }

  handleDone = (cancled) => () => {
    const { onDone } = this.props;

    if( istrue(cancled) ) {
      onDone();
    }

    const { original, attaching, idxList } = this.state;

    if( !attaching && isvalid(idxList) ) {
      let p = 0;
      original.filter((d, i) => {
        if( idxList[p] === i ) {
          p += 1;
        } else {
          d.selected = false;
        }
        return false;
      });
    }

    onDone( JSON.parse(JSON.stringify(original)) );
  }

  moveSelectPosition = (offset) => {
    const { beginIdx, items } = this.state;
    const newBegin = Math.min(Math.max(0, beginIdx + offset), Math.max(0, items.length - _visCount_));

    if( newBegin !== beginIdx ) {
      this.setState({ beginIdx:newBegin });
    }
  }

  handleKeyDown = (ev) => {
    // console.log('keydown', ev.keyCode, ev.key, ev.ctrlKey, ev.altKey, ev.shiftKey, ev.repeat);
    let processed = true;
    const { items } = this.state;

    switch( ev.keyCode ) {
      case 27: // Escape
        this.handleDone(true)();
        break;

      case 13: // Enter
        this.handleDone(false)();
        break;

      case 32: // Space
        break;

      case 37: // ArrowLeft
      case 38: // ArrowUp
        this.moveSelectPosition(-1); // go to previous column
        break;

      case 39: // ArrowRight
      case 40: // ArrowDown
        this.moveSelectPosition(1); // go to next column
        break;

      case 33: // PageUp
        this.moveSelectPosition(-_visCount_);
        break;

      case 34: // PageDown
        this.moveSelectPosition(_visCount_);
        break;

      case 36: // Home
        this.setState({ beginIdx:0 });
        break;

      case 35: // End
        this.setState({ beginIdx:Math.max(0, items.length - _visCount_) });
        break;

      default:
        processed = false;
        break;
    }

    if( processed && ev.preventDefault ) {
      ev.preventDefault();
      ev.stopPropagation();
    }
  }

  render () {
  	const { top, right } = this.props;
    const { items, sKeyword, drawKey, beginIdx, selectedCount, attaching } = this.state;
    const kwOn = sKeyword !== '';

    // console.log('render', selectedCount, items.length);

    const optTag = [];
    optTag.push(
      <FullCheckBox key={`fcb-${drawKey}-all`} title={kwOn ? '모든 검색 결과 선택' : '모두 선택'} checked={selectedCount === items.length} intermediated={selectedCount !== 0 && selectedCount < items.length} onChanged={this.handleFilterChanged(-1)} />
    );

    if( kwOn ) {
      optTag.push(
        <FullCheckBox key={`fcb-${drawKey}-attach`} title={'필터에 현재 선택 내용 추가'} checked={attaching} onChanged={this.handleFilterChanged(-2)} />
      );
    }

    const tags = [];
    for(let idx = beginIdx; tags.length < _visCount_ && idx < items.length; ++idx) {
      const o = items[idx];
      tags.push(
        <FullCheckBox key={`fcb-${drawKey}-${idx}`} title={o.title} checked={o.selected} color={o.color} onChanged={this.handleFilterChanged(idx)} />
      );
    }

    const cw = 240,
          itemBoxHeight = tags.length * _lh_,
          ch = itemBoxHeight + 1 + _lh_ * (itemBoxHeight > 0 ? optTag.length + 1 : 1) + (optTag.length > 0 ? 1 : 0); // 1: box margine. _lh_ * x (search box, select all, 필터에 현재 선택 내용 추가)
    const vScroll = itemBoxHeight + 1 < items.length * _lh_;

  	return (
  		<div
        className="columnFilterBox"
        style={{ top:(top - 1), left:Math.max(0, (right - cw + 1)), width:cw, height:ch }}
        onKeyDown={this.handleKeyDown}
      >
        <input type="text"
          className="filterSearchBox"
          placeholder={'Search...'}
          value={sKeyword}
          autoFocus={true}
          style={{ width:cw - _lh_, height:_lh_ }}
          onChange={this.handleChangeKeyword}
        />
        <div className="filterDoneButton"
          style={{ width:_lh_-2, height:_lh_-2, right:_lh_-1 }}
          onClick={this.handleClear}
        >
          <RiCloseLine size="16" />
        </div>
        <div className="filterDoneButton"
          style={{ width:_lh_-2, height:_lh_-2, right:1 }}
          onClick={this.handleDone(false)}
        >
          <RiCheckLine size="16" />
        </div>
        { tags.length > 0 && optTag.length > 0 && <div className="filterOptionBox" style={{ width:cw, height:(_lh_ * optTag.length + 1) }}>{ optTag.map((t) => t) }</div> }
        <div className="filterValueBox" onWheel={this.handleValueWheel}>
          { tags.length === 0 && <div className="filterValueNoItems">No items...</div> }
          { tags.length > 0 &&
            <div className="filterValueItems" style={{ width:(cw - 2 - (vScroll ? _scrollBarWith_ : 0)), height:itemBoxHeight }} >
              { tags.map((t) => t) }
            </div>
          }
          { vScroll &&
            <DataGridScrollBar
              barHeight={itemBoxHeight}
              barWidth={_scrollBarWith_}
              vertical={true}
              onPositionChanged={this.handleVScrollChanged}
              initialPosition={beginIdx}
              visibleCount={Math.floor(itemBoxHeight / _lh_)}
              total={items.length}
              fitted={true}
              adjust={top - 1 + _lh_}
            />
          }
        </div>
      </div>
  	);
  }
}


export default ColumnFilter;
export { ColumnFilter };
