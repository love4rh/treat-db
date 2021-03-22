import React, { Component } from 'react';
import PropTypes from 'prop-types';

import { isvalid, nvl } from './common.js';

import './styles.scss';



export const _scrollBarWith_ = 9;


const makeScrollState =  (props, state, dimChanged) => {
  if( isvalid(state) && !dimChanged ) {
    const { initialPosition } = props;
    const { ratio } = state;
    const thumbPos = initialPosition * ratio;

    return { initialPosition, thumbPos };
  }

  const { vertical, barWidth, barHeight, initialPosition, total, visibleCount } = props;

  const adjTotal = total < visibleCount ? total : total - visibleCount;
  const thumbSize = Math.max(_scrollBarWith_ * 3, barHeight * visibleCount / total);  // 전체 대비 화면에 표시될 수 있는 데이터 양의 비율
  const movableLen = barHeight - thumbSize;
  const ratio = movableLen / adjTotal; // 전체 값을 이동 가능한 영역에 맞추기 위한 비율
  const pageSize = visibleCount * ratio;
  let thumbPos = initialPosition * ratio;

  if( thumbPos + thumbSize > barHeight ) {
    thumbPos -= thumbPos + thumbSize - barHeight;
  }

  return {
    initialPosition, barHeight, barWidth, vertical, total, visibleCount,
    thumbPos, thumbSize, pageSize, ratio, movableLen
  };
}


class DataGridScrollBar extends Component {
  static propTypes = {
    initialPosition: PropTypes.number.isRequired, // Thumb 초기 표시 위치
    onPositionChanged: PropTypes.func.isRequired, // 스크롤 이동 이벤트 처리 핸들러
    vertical: PropTypes.bool.isRequired,          // 세로 스크롤바 여부. false이면 가로 스크롤바임.
    total: PropTypes.number.isRequired,           // 표시해야 할 전체 데이터 양
    visibleCount: PropTypes.number.isRequired,    // 현재 화면에 표시할 수 있는 데이터 양
    barHeight: PropTypes.number.isRequired,       // 긴 폭 (움직일 수 있는 거리임)
    barWidth: PropTypes.number,                   // 좁은 폭
    fitted: PropTypes.bool,                       // 표시 영역이 아이템 크기로 나눠 떨어지는지 여부
    marker: PropTypes.array                       // 다른 색으로 마킹할 레코드 목록. 없을 수 있음
  };

  constructor (props) {
    super(props);

    const initState = makeScrollState(props, null, true);

    this.state = {
      lastMousePos: null,
      ...initState
    };
  }

  static getDerivedStateFromProps (nextProps, prevState) {
    const { barWidth, barHeight, initialPosition, total, visibleCount } = nextProps;

    const dimChanged = prevState.barHeight !== barHeight || prevState.barWidth !== barWidth
      || prevState.total !== total || prevState.visibleCount !== visibleCount;

    if( dimChanged || prevState.initialPosition !== initialPosition ) {
      // 변경이 필요한 것만 반환
      const nextState = makeScrollState(nextProps, prevState, dimChanged);
      // console.log('CHECKTTT', nextState);
      return nextState;
    }

    return null; // null을 리턴하면 따로 업데이트 할 것은 없다라는 의미
  }

  handleDblClick = (ev) => {
    const { onPositionChanged } = this.props;

    if( !isvalid(onPositionChanged) ) {
      return;
    }

    ev.preventDefault();
    ev.stopPropagation();

    const { total, visibleCount, fitted } = this.props;
    const { thumbPos, thumbSize, vertical } = this.state;

    const target = ev.currentTarget;
    const p = vertical ? ev.clientY - target.offsetTop : ev.clientX - target.offsetLeft;

    // Thumb 클릭 시 스킵
    if( thumbPos <= p && p <= thumbPos + thumbSize ) {
      return;
    }

    if( thumbPos < p ) { // pagedown (증가)
      onPositionChanged('position', total - visibleCount + (fitted ? 0 : 1));
    } else if( thumbPos > p ) { // pageup (감소)
      onPositionChanged('position', 0);
    }
  }

  handleMouseDown = (ev) => {
    ev.preventDefault();
    ev.stopPropagation();

    const { total, visibleCount, fitted, adjust } = this.props;
    const { thumbPos, vertical, pageSize, ratio } = this.state;

    // const { shiftKey } = ev; // altKey, ctrlKey,
    const target = ev.currentTarget;
    const p = vertical ? ev.clientY - nvl(adjust, target.offsetTop) : ev.clientX - nvl(adjust, target.offsetLeft);

    let ns, nt;
    if( thumbPos < p ) { // pagedown (증가)
      nt = 'pagedown';
      ns = Math.min(total - visibleCount + (fitted ? 0 : 1), (thumbPos + pageSize) / ratio);
    } else {
      nt = 'pageup';
      ns = Math.max(0, (thumbPos - pageSize) / ratio);
    }

    this.setState({ initialPosition:ns, thumbPos:ns * ratio })

    if( this.props.onPositionChanged ) {
      this.props.onPositionChanged(nt, ns);
    }
  }

  handleMouseDownThumb = (ev) => {
    ev.preventDefault();
    ev.stopPropagation();

    const { thumbPos, vertical, } = this.state;

    // const { shiftKey } = ev; // altKey, ctrlKey,
    const target = ev.currentTarget;

    // capturing mouse event (refer: http://code.fitness/post/2016/06/capture-mouse-events.html)
    document.body.style['pointer-events'] = 'none';
    document.addEventListener('mousemove', this.handleMouseMove, { capture: true });
    document.addEventListener('mouseup', this.handleMouseUp, { capture: true });

    const pos = vertical ? ev.clientY : ev.clientX;
    const offset = vertical ? target.offsetTop : target.offsetLeft;

    this.setState({ lastMousePos:{ pos, offset, thumbPos } });
  }

  handleMouseMove = (ev) => {
    ev.preventDefault();
    ev.stopPropagation();

    if( this.state.lastMousePos === -1 ) {
      return;
    }

    const { total, visibleCount, fitted } = this.props;
    const { vertical, lastMousePos, ratio } = this.state;

    const target = ev.target;
    const p = vertical ? ev.clientY - target.offsetTop : ev.clientX - target.offsetLeft;

    const newTB = lastMousePos.thumbPos + (p - lastMousePos.pos);
    const ns = Math.max(Math.min(Math.floor(newTB / ratio), total - visibleCount + (fitted ? 0 : 1)), 0);

    // console.log('mousemove', p, JSON.stringify(lastMousePos), newTB, ns);

    this.setState({ initialPosition:ns, thumbPos:(ns * ratio) });

    if( this.props.onPositionChanged ) {
      this.props.onPositionChanged('position', ns);
    }
  }

  handleMouseUp = (ev) => {
    if( isvalid(this.state.lastMousePos) ) {
      ev.preventDefault();
      ev.stopPropagation();

      document.body.style['pointer-events'] = 'auto';
      document.removeEventListener('mousemove', this.handleMouseMove, { capture: true });
      document.removeEventListener('mouseup', this.handleMouseUp, { capture: true });
      this.setState({ lastMousePos: null });
    }
  }

  render () {
    // const { marker } = this.props;
    const { barHeight, barWidth, vertical, thumbPos, thumbSize } = this.state;

    let barStyle = {}, thumbStyle = {};
    const adjThumb = 1;

    if( vertical ) {
      barStyle = { height:barHeight, width:barWidth, borderLeft:'1px solid #61615b' };
      thumbStyle = { top:thumbPos, left:0, width:(barWidth - adjThumb), height: thumbSize };
    } else {
      barStyle = { height:barWidth, width:barHeight };
      thumbStyle = { top:0, left:thumbPos, width: thumbSize, height:(barWidth - adjThumb) };
    }

    /*
    { marker &&
      <div className="sbMarker" style={{ top:200, left:1, width:barWidth, height:100 }} />
    } // */

    return (
      <div
        className="sbMain"
        style={barStyle}
        onMouseDown={this.handleMouseDown}
        onDoubleClick={this.handleDblClick}
      >
        <div className="sbThumb" onMouseDown={this.handleMouseDownThumb} style={thumbStyle} />
      </div>
    );
  }
}

export default DataGridScrollBar;
export { DataGridScrollBar };
