import React, { Component } from 'react';
import PropTypes from 'prop-types';

import './LineMap.scss';



const calculateLineMapDim = (beginRow, endRow, cw, ch, total) => {
  const lh = ch <= total ? 1 : ch / total;
  const adjTotal = total - (endRow - beginRow);
  const visibleCount = Math.floor(ch / lh + 0.5);
  const mapBegin = Math.floor(beginRow / adjTotal * (total - visibleCount));

  const rectHeight = Math.max((endRow - beginRow) / adjTotal * ch, 12);
  const rectTop = Math.min(beginRow / adjTotal * ch, ch - rectHeight);

  return {
    lh,
    cw,
    ch,
    beginRow,
    endRow,
    visibleCount,
    mapBegin,
    rectTop,
    rectHeight
  };
}


class LineMap extends Component {
	static propTypes = {
    beginRow: PropTypes.number,
    endRow: PropTypes.number,
		colorMap: PropTypes.array,
    onPositionChanged: PropTypes.func,
    rowFilter: PropTypes.array
  };

  constructor (props) {
    super(props);

    const { beginRow, endRow, colorMap, rowFilter } = props;

    this.state = {
      rowFilter,
      ...calculateLineMapDim(beginRow, endRow, 100, 100, rowFilter ? rowFilter.length : (colorMap ? colorMap[0].length : 100))
    };

    this._refMain = React.createRef();
  }

  componentDidMount () {
    window.addEventListener('resize', this.handleResize);
    this.handleResize();
  }

  static getDerivedStateFromProps(nextProps, prevState) {
    if( prevState.beginRow !== nextProps.beginRow || prevState.endRow !== nextProps.endRow || nextProps.rowFilter !== prevState.rowFilter ) {
      const rf = nextProps.rowFilter;
      // 변경이 필요한 것만 반환
      return calculateLineMapDim(nextProps.beginRow, nextProps.endRow, prevState.cw, prevState.ch,
        rf ? rf.length : nextProps.colorMap[0].length);
    }

    return null; // null을 리턴하면 따로 업데이트 할 것은 없다라는 의미
  }

  componentWillUnmount () {
    window.removeEventListener("resize", this.handleResize);
  }

  handleResize = () => {
    const { beginRow, endRow, colorMap } = this.props;
    const { clientWidth, clientHeight } = this._refMain.current;
    this.setState( calculateLineMapDim(beginRow, endRow, clientWidth, clientHeight, (colorMap ? colorMap[0].length : 100)) );
  }

  onMouseDown = (ev) => {
    const { onPositionChanged, colorMap } = this.props;
    if( !onPositionChanged || !colorMap ) {
      return;
    }

    const { mapBegin, lh } = this.state;
    const { offsetTop } = this._refMain.current;
    const y = ev.clientY - offsetTop - 1;

    if( y >= colorMap[0].length * lh ) {
      return;
    }

    onPositionChanged( Math.floor(Math.max(0, mapBegin + y / lh)) );
  }

  render () {
    const { rowFilter, colorMap } = this.props;
    const { cw, ch, visibleCount, mapBegin, rectTop, rectHeight, lh } = this.state;
    const lw = cw / 2;

    const tag = [];

    if( colorMap ) {
      for(let i = 0; i < visibleCount; ++i) {
        let idx = mapBegin + i;
        if( rowFilter ) {
          if( idx >= rowFilter.length ) {
            break;
          }
          idx = rowFilter[idx];
        }
        if( idx >= colorMap[0].length ) {
          break;
        }
        const y = i * lh;
        tag.push(<line key={`cmap-0-${idx}`} x1={0}  y1={y} x2={lw} y2={y} style={{ stroke:'#' + colorMap[0][idx], strokeWidth: lh, strokeOpacity: 0.8 }} />);
        tag.push(<line key={`cmap-1-${idx}`} x1={lw} y1={y} x2={cw} y2={y} style={{ stroke:'#' + colorMap[1][idx], strokeWidth: lh, strokeOpacity: 0.8 }} />);
      }
    }

  	return (
  		<div ref={this._refMain} className="lineMapBox">
        { colorMap &&
          <svg
            key={`linemap-${mapBegin}`}
            width={cw}
            height={ch}
            onMouseDown={this.onMouseDown}
          >
            { tag.map((t) => t) }
            <rect x={0} y={rectTop} width={cw} height={rectHeight}
              style={{ fill:'rgba(48, 47, 39, 0.6)' }}
            />
          </svg>
        }
  		</div>
  	);
  }
}

export default LineMap;
export { LineMap };
