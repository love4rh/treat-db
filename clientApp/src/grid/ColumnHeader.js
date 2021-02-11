import React, { Component } from 'react';
import PropTypes from 'prop-types';

import cn from 'classnames';
import { RiArrowDownSFill, RiFilterLine, RiPushpin2Line, RiPushpinLine } from 'react-icons/ri';

import './styles.scss';



class ColumnHeader extends Component {
  static propTypes = {
    index: PropTypes.number,
    left: PropTypes.number,
    selected: PropTypes.bool,
    showNumber: PropTypes.bool,
    title: PropTypes.string,
    width: PropTypes.number,
    hasFilter: PropTypes.bool,
    onColumnEvent: PropTypes.func, // Column 이벤트 핸들러. 필터(open, close), 컬럼 고정(pinned, unpinned)
    filtered: PropTypes.bool,
    fixedType: PropTypes.number, // 0: normal, 1: fixed, 2: last fixed
    fixable: PropTypes.bool, // Fixed 컬럼 가능여부
  }

  constructor (props) {
    super(props);

    this.state = {
      filtered: props.filtered,
      filterOpen: false,
    };
  }

  static getDerivedStateFromProps (nextProps, prevState) {
    if( nextProps.filtered !== prevState.filtered ) {
      return { filtered: nextProps.filtered };
    }

    return null;
  }

  handleContextMenu = (ev) => {
    // button - 0: left, 2: right
    // console.log('Header', ev);

    ev.preventDefault();
    ev.stopPropagation();

    const target = ev.currentTarget;
    let { x, y, width, height } = target.getBoundingClientRect();
    console.log('Mouse', ev.button, ev.clientX, ev.clientY, x, y, width, height);
  }

  handleMouseDown = (type) => (ev) => {
    ev.preventDefault();
    ev.stopPropagation();

    const { index, onColumnEvent, fixedType } = this.props;
    const { filterOpen } = this.state;

    if( filterOpen && type === 'filter' ) {
      if( onColumnEvent ) {
        onColumnEvent('close', { colIdx:index });
      }
      this.handleCloseFilter();
      return;
    }

    const target = ev.currentTarget;
    let { x, y, width, height } = target.getBoundingClientRect();

    x += window.pageXOffset + width;
    y += window.pageYOffset + height;

    switch( type ) {
    case 'filter':
      if( onColumnEvent ) {
        onColumnEvent('open', { colIdx:index, pos:{x, y}, cbClose:this.handleCloseFilter });
      }
      this.setState({ filterOpen: true });
      break;

    case 'pin':
      if( onColumnEvent ) {
        onColumnEvent(fixedType === 0 ? 'pinned' : 'unpinned', { colIdx:index, pos:{x, y} });
      }
      break;

    default:
      break;
    }
  }

  handleCloseFilter = () => {
    this.setState({ filterOpen: false });
  }

  render () {
    const { index, title, width, left, selected, rowHeight, hasFilter, showNumber, fixedType, fixable } = this.props;
    const { filterOpen, filtered } = this.state;

    const lineHeight = (rowHeight - 10) + 'px';
    const btnSize = rowHeight - 2;
    const wider = width > 30;

    const css = cn({ 'column':true, 'selectedHeader':selected, 'fixedLine':fixedType === 2 });

    return (
      <React.Fragment>
        { showNumber &&
          <div className={ css } style={{ top:0 , left, width, height:rowHeight }}>
            <div
              className="columnTitle"
              style={{ lineHeight:lineHeight, height:rowHeight, width }}
              onContextMenu={this.handleContextMenu}
            >
              {index + 1}
            </div>
            { fixable && wider &&
              <div
                className={ cn({ 'columnHederButton':true }) }
                style={{ height:rowHeight, width:btnSize }}
                onMouseDown={this.handleMouseDown('pin')}
              >
                { fixedType === 0 ? <RiPushpinLine size="14" color="#b0b1ab" /> : <RiPushpin2Line size="14" color="#b0b1ab" /> }
              </div>
            }
          </div>
        }
        <div className={ css } style={{ top:(showNumber ? rowHeight : 0), left, width, height:rowHeight }}>
          <div
            className="columnTitle"
            style={{ lineHeight:lineHeight, height:rowHeight, width:(width - (hasFilter ? btnSize : 0)) }}
            onContextMenu={this.handleContextMenu}
          >
            {title}
          </div>
          { hasFilter && wider&&
            <div
              className={ cn({ 'columnHederButton':true, 'columnHederButtonPressed':filterOpen }) }
              style={{ height:rowHeight, width:btnSize }}
              onMouseDown={this.handleMouseDown('filter')}
            >
              { filtered ? <RiFilterLine size="14" /> : <RiArrowDownSFill size="16" /> }
            </div>
          }
        </div>
      </React.Fragment>
    );
  }
}

export default ColumnHeader;
export { ColumnHeader };
