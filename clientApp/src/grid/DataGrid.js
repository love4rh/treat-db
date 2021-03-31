import React, { Component } from 'react';
import PropTypes from 'prop-types';

import cn from 'classnames';

import {
  isvalid, isundef, isBetween, nvl, calcDigitsWithCommas, tickCount, binarySearch,
  setCurrentActiveGrid, dismissActiveGrid, proxyCall
} from './common.js';

import { DataGridScrollBar, _scrollBarWith_ } from './DataGridScrollBar.js';

import { RowHeader } from './RowHeader.js';
import { ColumnHeader } from './ColumnHeader.js';
import { DataRecord } from './DataRecord.js';

import { ColumnFilter } from './ColumnFilter.js';
import { FindDialog } from './FindDialog.js';

import './styles.scss';



// Grid의 Mouse Action 상태 정의
const MA = {
  NORMAL: 0,
  SIZING: 1,
  SELCOL: 2,
  SELROW: 3,
  SELCELL: 4
};

// Grid에서 발생하는 Event 정의
const GridEvent = {
  CELL_SELECTED: 1,
  CELL_DBLCLICK: 2,
  COLUMN_SELECTED: 3,
  ROW_SELECTED: 4,
}

const _letterWidth_ = 12 * 8.5 / 16; // 32


/**
 * DataGrid.
 * Properties:
 * Column
 * @required  react-virtualized
 */
class DataGrid extends Component {
  static propTypes = {
    dataSource: PropTypes.object.isRequired,  // 표시할 원천 데이터
    showRowNumber: PropTypes.bool,  // 행번호 표시여부. 기본값 true
    showColumnNumber: PropTypes.bool,  // 컬럼번호 표시여부. 기본값 true
    userBeginRow: PropTypes.number,  // 처음 표시할 시작 위치
    fixableCount: PropTypes.number,  // Fixed 가능한 컬럼 개수
    height: PropTypes.number, // Grid의 높이. width도 같이 지정된 경우에만 적용됨
    width: PropTypes.number,  // Grid의 너비. height도 같이 지정된 경우에만 적용됨
    onEvent: PropTypes.func, // Grid에서 발생하는 이벤트를 전달하기 위한 속성
  }

  // 페이지 당 표시 가능한 레코드 개수 계산
  static CalcRowNumPerPage = (height, rowHeight, headerCount) => {
    return Math.ceil((height - rowHeight * headerCount) / rowHeight);
  }

  // Column의 초기 너비 계산
  static calcIntialColumnWidth = (ds, maxWidth) => {
    if( isundef(maxWidth) ) {
      maxWidth = 480;
    }

    const rowHeight = ds.getRowHeight();
    const columnCount = ds.getColumnCount();

    let widthSum = 0;
    const columnWidth = [0];

    for(let c = 0; c < columnCount; ++c) {
      let w = Math.max(50, Math.min(maxWidth, ds.getColumnName(c).length * _letterWidth_ + 16))
         + (ds.hasColumnFilterData && ds.hasColumnFilterData(c) ? rowHeight : 0);

      if( ds.getPreferedColumnWidth ) {
        w = ds.getPreferedColumnWidth(c);
      }

      widthSum += Math.min(Math.ceil(w), maxWidth);
      columnWidth.push(widthSum);
    }

    return columnWidth;
  }

  static recalculateDimension = (props, state, nw, nh, colWidth, fixedColIdx) => {
    const { dataSource, onVisibleAreaChanged, height, width } = props;
    const { cw, ch, showRowNumber, showColumnNumber, columnWidth, fixedColumn } = state;

    if( isundef(nw) ) nw = nvl(width, cw);
    if( isundef(nh) ) nh = nvl(height, ch);
    if( isundef(colWidth) && columnWidth ) colWidth = columnWidth;
    if( isundef(fixedColIdx) ) fixedColIdx = fixedColumn;

    const ds = dataSource;
    const rowHeight = ds.getRowHeight();
    const rowCount = ds.getRowCount();
    const rowPerHeight = DataGrid.CalcRowNumPerPage(nh, rowHeight, (showColumnNumber ? 2 : 1));

    const fixedColWidth = colWidth && fixedColIdx > 0 ? colWidth[fixedColIdx] : 0;
    const headerWidth = showRowNumber ? calcDigitsWithCommas(rowCount) * _letterWidth_ + 24 : 0;
    const chHeight = rowHeight * (showColumnNumber ? 2 : 1); // 컬럼 헤더 전체 높이
    const rhWidth = headerWidth + fixedColWidth; // Row 헤더 너비
    const rhHeight = nh - chHeight; // 레코드 헤더 높이 (데이터 표시 영역 높이)
    const chWidth = nw - rhWidth; // 컬럼 헤더 너비 (데이터 표시 영역 너비)
    const vScroll = rowCount > rowPerHeight - 1;
    const hScroll = chWidth + fixedColWidth - (vScroll ? _scrollBarWith_ : 0) < (colWidth ? colWidth[colWidth.length - 1] : 0);

    if( onVisibleAreaChanged && nh !== state.ch ) {
      const beginRow = state.beginRow;
      onVisibleAreaChanged(beginRow, Math.min(beginRow + rowPerHeight, ds.getRowCount()));
    }

    return {
      ds,
      dsModifiedTick: proxyCall(ds, 'updatedTime'),
      ch: nh,
      cw: nw,
      columnWidth: colWidth,
      fixedColumn: fixedColIdx,
      rowPerHeight,   // 화면에 나타낼 수 있는 레코드 개수
      fixedColWidth,  // 고정 영역의 너비. 고정 영역이 없으면 0임.
      headerWidth,    // 헤더 셀의 너비 (행번호 표시 영역의 너비)
      chHeight,       // 컬럼 헤더의 전체 높이
      rhWidth,        // 고정 영역을 포함한 레코드 헤더의 너비
      rhHeight,       // 레코드 헤더 높이 (데이터 표시 영역 높이와 동일함)
      chWidth,        // 컬럼 헤더 너비 (데이터 표시 영역 너비)
      hScroll,        // Horizontal Scroll 표시 여부
      vScroll         // Vertical Scroll 표시 여부
    };
  }

  constructor (props) {
    super(props);

    const { dataSource, userBeginRow, fixedColumn, showRowNumber, showColumnNumber, height, width } = this.props;
    const columnWidth = DataGrid.calcIntialColumnWidth(dataSource, 480);

    this.state = {
      ds: dataSource,
      beginRow: nvl(userBeginRow, 0),
      userBeginRow: nvl(userBeginRow, 0),
      scrollLeft: 0,
      selectedRange: { row:0, col:0, row2:0, col2:0 },
      overCell: { row:-1, col:-1, colEdge:false, rowEdge:false },
      preStatus: MA.SELCELL,
      status: MA.NORMAL,
      statusParam: {},
      loading: false,
      activeFilter: -1,
      filterPos: null,
      fixedColumn: nvl(fixedColumn, 0),
      clickTick: [0, 0],
      inFinding: false,
      cw: nvl(width, 0),
      ch: nvl(height, 0),
      showRowNumber: isvalid(showRowNumber) ? showRowNumber : true,
      showColumnNumber: isvalid(showColumnNumber) ? showColumnNumber : true
    };

    // recalculateDimension에서 this.state를 사용하기 때문에 나눠서 정의했음.
    this.state = {
      ...this.state,
      ...DataGrid.recalculateDimension(props, this.state, 512, 512, columnWidth)
    };

    this.dataFetchJob = null;
    this._refMain = React.createRef();

    this._elementRef = {
      rowHeader: React.createRef(),
      columnArea: React.createRef(),
      dataContainer: React.createRef()
    };

    this._eventHold = false;
    this._findRes = {};
  }

  componentDidMount () {
    setCurrentActiveGrid(this);

    const { height, width } = this.props;

    if( isvalid(height) && isvalid(width) ) {
      this.setState( DataGrid.recalculateDimension(this.props, this.state, width, height) );
      return;
    }

    window.addEventListener('resize', this.onResize);

    const { clientWidth, clientHeight } = this._refMain.current;
    this.setState( DataGrid.recalculateDimension(this.props, this.state, clientWidth, clientHeight) );
  }

  static getDerivedStateFromProps(nextProps, prevState) {
    let modified = false;
    let newState = {};
    let columnWidth = null;
    const nextDS = nextProps.dataSource;

    if( nextDS !== prevState.ds ) {
      modified = true;
      columnWidth = DataGrid.calcIntialColumnWidth(nextDS, 480);
      newState = DataGrid.recalculateDimension(nextProps, prevState, null, null, columnWidth);
      newState.beginRow = 0;
    } else if( isvalid(nextProps.height) && isvalid(nextProps.width) && nextProps.height !== prevState.clientHeight && nextProps.width !== prevState.clientWidth ) {
      modified = true;
      newState = DataGrid.recalculateDimension(nextProps, prevState, nextProps.width, nextProps.height, columnWidth);
    } else if( prevState.dsModifiedTick !== proxyCall(nextDS, 'updatedTime') ) {
      modified = true;
      newState = DataGrid.recalculateDimension(nextProps, prevState);
    }

    if( prevState.userBeginRow !== nextProps.userBeginRow  ) {
      modified = true;
      newState.beginRow = nextProps.userBeginRow;
      newState.userBeginRow = nextProps.userBeginRow;
    }

    return modified ? newState : null; // null을 리턴하면 따로 업데이트 할 것은 없다라는 의미
  }

  componentWillUnmount () {
    dismissActiveGrid(this);

    if( isundef(this.props.height) || isundef(this.props.width) ) {
      window.removeEventListener('resize', this.onResize);
    }

    if( isvalid(this.dataFetchJob) ) {
      clearTimeout(this.dataFetchJob);
    }
  }

  onResize = () => {
    const { clientWidth, clientHeight } = this._refMain.current;
    this.setState( DataGrid.recalculateDimension(this.props, this.state, clientWidth, clientHeight) );
  }

  applyColumnOption = ({ columnWidth, fixedColumn }) => {
    const { keyword, caseSensitive } = this._findRes;

    this._findRes = { keyword, caseSensitive };
    this.setState( DataGrid.recalculateDimension(this.props, this.state, null, null, columnWidth, fixedColumn) );
    this.setScrollLeft(0);
  }

  setColumnWidth = (col, size) => {
    const { dataSource } = this.props;
    const { cw, ch, columnWidth, scrollLeft, chWidth, vScroll } = this.state;
    const old = columnWidth[col + 1] - columnWidth[col];

    if( old === size ) {
      return;
    }

    // 표시 가능 화면 크기
    const adjDataWidth = chWidth - (vScroll ? _scrollBarWith_ : 0);

    for(let c = col; c < dataSource.getColumnCount(); ++c) {
      columnWidth[c + 1] += size - old;
    }

    this.setState( DataGrid.recalculateDimension(this.props, this.state, cw, ch, columnWidth) );

    if( scrollLeft + adjDataWidth > columnWidth[columnWidth.length - 1] ) {
      this.setScrollLeft(columnWidth[columnWidth.length - 1] - adjDataWidth);
    }
  }

  getColumnWidth = (col) => {
    const { columnWidth } = this.state;
    return columnWidth[col + 1] - columnWidth[col];
  }

  changeState = (state) => {
    console.log(state);

    if( state.type === 'click' ) {
      if( state.target === 'cell' ) {
        this.setState({ selectedRange: state.value });
      }
    }
  }

  isSelectedColumn = (col) => {
    const { selectedRange } = this.state;
    const col2 = nvl(selectedRange.col2, selectedRange.col);

    return isBetween(col, Math.min(selectedRange.col, col2), Math.max(selectedRange.col, col2) + 1);
  }

  isSelectedRow = (row) => {
    const { selectedRange } = this.state;
    const row2 = nvl(selectedRange.row2, selectedRange.row);

    return isBetween(row, Math.min(selectedRange.row, row2), Math.max(selectedRange.row, row2) + 1);
  }

  isSelected = (col, row) => {
    return this.isSelectedRow(row) && this.isSelectedColumn(col);
  }

  copySelected = () => {
    const ds = this.props.dataSource;
    const sel = this.state.selectedRange;

    const
      r1 = Math.max(0, Math.min(sel.row, nvl(sel.row2, sel.row))),
      r2 = Math.min(ds.getRowCount() - 1, Math.max(sel.row, nvl(sel.row2, sel.row))),
      c1 = Math.max(0, Math.min(sel.col, nvl(sel.col2, sel.col))),
      c2 = Math.min(ds.getColumnCount() - 1, Math.max(sel.col, nvl(sel.col2, sel.col)))
    ;

    const copiedCell = (c2 - c1 + 1) * (r2 - r1 + 1);

    if( copiedCell > 10000 ) {
      console.log('TOO BIG TO BE COPIED', copiedCell);
      // return;
    }

    // TODO 아래 텍스트를 데이터소스에 요청해 받기
    let copyText = '';
    for(let r = r1; r <= r2; ++r) {
      copyText += ds.getCellValue(c1, r);
      for(let c = c1 + 1; c <= c2; ++c) {
        copyText += '\t';
        copyText += ds.getCellValue(c, r);
      }

      copyText += '\n';
    }

    const t = document.createElement('textarea');
    document.body.appendChild(t);
    t.value = copyText;
    t.select();
    document.execCommand('copy');
    document.body.removeChild(t);
  }

  calcNewBeginRow = (offsetY) => {
    const { dataSource } = this.props;
    const { beginRow, rowPerHeight } = this.state;
    const rowCount = dataSource.getRowCount();

    // beginRow: [0, rowCount - rowPerHeight + 1]
    const newBegin = Math.min(Math.max(0, beginRow + offsetY), Math.max(0, rowCount - rowPerHeight + 1));

    if( beginRow === newBegin )
      return -1;

    return newBegin;
  }

  // direction: -1 왼쪽, 1 오른쪽
  _ensureVisibleColumn = (col, direction) => {
    const { columnWidth, scrollLeft, headerWidth, cw, fixedColumn, vScroll } = this.state;

    const adjFixed = columnWidth[fixedColumn];
    const dataWidth = cw - headerWidth - adjFixed,
      vX1 = scrollLeft,
      vX2 = vX1 + dataWidth;

    // 선택된 컬럼이 처음부터 보이도록 스크롤 위치 조정
    if( columnWidth[col] - adjFixed < vX1 || columnWidth[col + 1] - adjFixed > vX2 ) {
      return direction < 0
        ? columnWidth[col] - adjFixed
        : Math.max(0, columnWidth[col + 1] - dataWidth - adjFixed + (vScroll ? _scrollBarWith_ : 0));
    }

    return null;
  }

  ensureVisibleColumn = (col, direction) => {
    const nv = this._ensureVisibleColumn(col, direction);
    if( nv !== null ) {
      this.setScrollLeft( nv );
    }
  }

  ensureVisibleCell = (col, row, forward, select) => {
    const { beginRow, rowPerHeight } = this.state;
    let ns = this._ensureVisibleColumn(col, forward ? 1 : -1);

    if( ns === null ) {
      ns = {};
    }

    if( row < beginRow || row >= beginRow + rowPerHeight ) {
      const ts = this._setBeginRow(row - Math.floor(rowPerHeight / 2));
      ns = { ...ns, ...ts };
    }

    if( select ) {
      ns = { ...ns, selectedRange:{ col, row, col2:col, row2:row } };
    }

    if( ns != null ) {
      this.setState(ns);
    }
  }

  moveCellPosition = (offsetX, offsetY, selecting) => {
    if( offsetX === 0 && offsetY === 0 ) {
      return false;
    }

    const { dataSource } = this.props;
    const { beginRow, selectedRange, rowPerHeight, ch } = this.state;

    const height = ch;
    const rowFitted = 0 === (height % dataSource.getRowHeight());
    const rowCount = dataSource.getRowCount();

    const newPos = { ...selectedRange,
      col: Math.min(Math.max(0, selectedRange.col + offsetX), dataSource.getColumnCount() - 1),
      row: Math.min(Math.max(0, selectedRange.row + offsetY), dataSource.getRowCount() - 1)
    };

    if( selectedRange.col === newPos.col && selectedRange.row === newPos.row )
      return false;

    if( !selecting ) {
      newPos.row2 = newPos.row;
      newPos.col2 = newPos.col;
    }

    // 선택된 컬럼이 처음부터 보이도록 스크롤 위치 조정
    this.ensureVisibleColumn(newPos.col, offsetX);

    let newBegin = beginRow;
    if( newPos.row < beginRow || newPos.row >= beginRow + rowPerHeight - 1 ) {
      if( offsetY < 0 ) {
        newBegin = Math.max(0, Math.min(newPos.row, Math.max(0, rowCount - rowPerHeight + 1)));
      } else {
        newBegin = Math.max(rowPerHeight, Math.min(newPos.row, rowCount) + (rowFitted ? 1 : 2)) - rowPerHeight;
      }
    }

    if( newBegin !== beginRow ) {
      this.setBeginRow(newBegin);
    }

    this.setState({ selectedRange: newPos });
    this.relayEvent(GridEvent.CELL_SELECTED, { column:newPos.col, row:newPos.row, status:MA.SELCELL });

    return true;
  }

  _setBeginRow = (newBegin) => {
    const { onVisibleAreaChanged } = this.props;
    const { ds, rowPerHeight } = this.state;

    const rowCount = ds.getRowCount();
    newBegin = Math.max(Math.min(newBegin, rowCount - rowPerHeight + 1), 0);

    // 표시 영역 데이터 존재 여부 체크
    const areaInvalid = !ds.isValid || !ds.isValid(newBegin, newBegin + rowPerHeight);

    if( areaInvalid ) {
      if( isvalid(this.dataFetchJob) ) {
        clearTimeout(this.dataFetchJob);
      }

      this.dataFetchJob = setTimeout(() => {
        ds.getMore(newBegin, rowPerHeight, () => {
          this.dataFetchJob = null;
          if( onVisibleAreaChanged ) {
            onVisibleAreaChanged(newBegin, Math.min(newBegin + rowPerHeight, rowCount));
          }
          this.setState({ beginRow:newBegin, loading:false });
        });
      }, 200);
    } // end of if

    if( onVisibleAreaChanged ) {
      onVisibleAreaChanged(newBegin, Math.min(newBegin + rowPerHeight, rowCount));
    }

    return { beginRow:newBegin, loading:areaInvalid };
  }

  setBeginRow = (newBegin) => {
    this.setState( this._setBeginRow(newBegin) );
  }

  _setScrollLeft = (left) => {
    const { columnWidth, chWidth, vScroll } = this.state;

    left = Math.max(0, Math.min(left, columnWidth[columnWidth.length - 1] - (chWidth - (vScroll ? _scrollBarWith_ : 0)) ));

    this._elementRef['columnArea'].current.scrollLeft = left;
    this._elementRef['dataContainer'].current.scrollLeft = left;
    
    return { scrollLeft: left };
  }

  setScrollLeft = (left) => {
    if( this.state.scrollLeft === left ) {
      return;
    }

    this.setState( this._setScrollLeft(left) );
  }

  handleHScrollChanged = (type, start) => {
    this.setScrollLeft(start);
  }

  handleVScrollChanged = (type, start) => {
    const { beginRow, rowPerHeight } = this.state;
    // console.log('handleVScrollChanged', type, start);

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
  }

  onFocus = () => {
    setCurrentActiveGrid(this);
  }

  onDataAreaWheel = (ev) => {
    // ev.preventDefault();
    ev.stopPropagation();

    // down: +, up: -
    const offsetY = (ev.deltaY < 0 ? -1 : 1) * Math.ceil(Math.abs(ev.deltaY) / 80);

    // console.log('onDataAreaWheel', ev.deltaX, ev.deltaY, ev.deltaMode, offsetY);

    const newBegin = this.calcNewBeginRow(offsetY);

    if( Math.abs(ev.deltaX) >= 1 ) {
      this.setScrollLeft(this.state.scrollLeft + ev.deltaX);  
    }

    // console.log('onDataAreaWheel', newBegin, ev.deltaY, offsetY);

    if( newBegin === -1 )
      return;

    this.setBeginRow(newBegin);
  }

  onKeyDown = (ev) => {
    if( this._eventHold ) {
      return;
    }
    // console.log('keydown', ev.keyCode, ev.key, ev.ctrlKey, ev.altKey, ev.shiftKey, ev.repeat);
    let processed = true;

    const { keyCode, ctrlKey, shiftKey } = ev;
    const { ds, selectedRange, rowPerHeight, inFinding } = this.state;
    const colCount = ds.getColumnCount(),
          rowCount = ds.getRowCount();

    switch( keyCode ) {
      case 13: // Enter
        // TODO selectedRange.col, selectedRange.row
        break;
      case 27: // Escape. TODO 모든 상태를 취소하도록 조치
        if( inFinding ) {
          this.setState({ inFinding: false });
        }
        break;
      case 37: // ArrowLeft
        if( ctrlKey )
          this.setColumnWidth(selectedRange.col, this.getColumnWidth(selectedRange.col) - 2); // shrink column size
        else 
          this.moveCellPosition(-1, 0, shiftKey); // go to previous column
        break;
      case 39: // ArrowRight
        if( ctrlKey )
          this.setColumnWidth(selectedRange.col, this.getColumnWidth(selectedRange.col) + 2); // grow column size
        else
          this.moveCellPosition(1, 0, shiftKey); // go to next column
        break;
      case 38: // ArrowUp
        this.moveCellPosition(0, -1, shiftKey); // go to previous row
        break;
      case 40: // ArrowDown
        this.moveCellPosition(0, 1, shiftKey); // go to next row
        break;
      case 33: // PageUp
        if( ctrlKey )
          this.moveCellPosition(-colCount, -rowCount, false); // go to (0, 0), but not worked
        else
          this.moveCellPosition(0, - rowPerHeight + 2, false); // go to previous page
        break;
      case 34: // PageDown
        if( ctrlKey )
          this.moveCellPosition(colCount, rowCount, false); // go to last cell, but not worked
        else
          this.moveCellPosition(0, rowPerHeight - 2, false); // go to next page
        break;
      case 36: // Home
        if( ctrlKey || selectedRange.col === 0 )
          this.moveCellPosition(0, -rowCount, false); // go to last row
        else
          this.moveCellPosition(-colCount, 0, false);
        break;
      case 35: // End
        if( ctrlKey || selectedRange.col === colCount - 1)
          this.moveCellPosition(0, rowCount, false);
        else
          this.moveCellPosition(colCount, 0, false);
        break;
      case 114: // F3
        this.onFindEvent(shiftKey ? 'prev' : 'next');
        break;
      default:
        processed = false;
        break;
    }

    if( !processed && ctrlKey ) {
      processed = true;

      if( keyCode === 67 ) { // ctrl+c: copy
        this.copySelected();
      } else if( keyCode === 65 ) { // ctrl+a: select all
        this.setState({ selectedRange:{col:0, row:0, col2:colCount - 1, row2:rowCount - 1} });
      } else if( keyCode === 70 ) { // ctrl+f: find
        this.setState({ inFinding:true });
      } else
        processed = false;
    }

    if( processed && ev.preventDefault ) {
      ev.preventDefault();
      ev.stopPropagation();
    }
  }

  hitTest = (x, y) => {
    let col = null, row = null, colEdge = false, rowEdge = false;

    const edgeMargin = 2;

    const { ds, showColumnNumber, columnWidth, beginRow, scrollLeft, headerWidth, cw, ch, fixedColumn } = this.state;
    const width = cw, height = ch;

    const rowHeight = ds.getRowHeight(),
      chHeight = rowHeight * (showColumnNumber ? 2 : 1),
      rhWidth = headerWidth;

    if( (width - rhWidth) < columnWidth[columnWidth.length - 1] && y > height - _scrollBarWith_ ) {
      return null;
    }

    let rhHeight = height - chHeight;

    if( (rhHeight % rowHeight) === 0 )
      rhHeight -= _scrollBarWith_;

    // find column index matching to x
    if( x <= rhWidth + edgeMargin ) {
      col = -1;
      colEdge = (x >= rhWidth - edgeMargin);
    } else {
      // 고정된 컬럼이 있는 경우
      x -= rhWidth;
      if( fixedColumn > 0 && x < columnWidth[fixedColumn] + edgeMargin ) {
        for(let c = 1; c <= fixedColumn; ++c) {
          if( x <= columnWidth[c] + edgeMargin ) {
            col = c - 1;
            colEdge = (x >= columnWidth[c] - edgeMargin);
            break;
          }
        }
      } else {
        x += scrollLeft;
        // TODO change find-logic to using binary search.
        for(let c = fixedColumn + 1; c <= ds.getColumnCount(); ++c) {
          if( x <= columnWidth[c] + edgeMargin ) {
            col = c - 1;
            colEdge = (x >= columnWidth[c] - edgeMargin);
            break;
          }
        }
      }
    }

    // find row index matching to y
    if( y <= chHeight + edgeMargin ) {
      row = -1;
      rowEdge = (y >= chHeight - edgeMargin);
    } else {
      y -= chHeight;
      row = Math.floor(y / rowHeight)
      rowEdge = Math.abs(y - row * rowHeight) <= edgeMargin;
      row += beginRow;
    }

    return row >= ds.getRowCount() ? null : { col: col, row: row, colEdge: colEdge, rowEdge: rowEdge };
  }

  onMouseEvent = (ev) => {
    if( this._eventHold ) {
      return;
    }

    ev.preventDefault();
    ev.stopPropagation();

    const { shiftKey } = ev; // altKey, ctrlKey,
    const target = ev.currentTarget,
      x = ev.clientX - target.offsetLeft,
      y = ev.clientY - target.offsetTop
    ;

    const cell = this.hitTest(x, y)

    if( cell === null )
      return;

    const { ds, preStatus, status, statusParam, fixedColumn, clickTick, cw } = this.state;

    // colCount, rowCount 모두 inclusive임
    const colCount = ds.getColumnCount() - 1, rowCount = ds.getRowCount() - 1;

    switch( ev.type ) {
    // Mouse Down
    case 'mousedown':
      // console.log('ScrollLeft', this.state.scrollLeft);
      // 유효한 컬럼이라면, 처음 부분이 화면에 표시될 수 있도록 스크롤 조정
      if( fixedColumn <= cell.col && cell.col < colCount ) {
        this.ensureVisibleColumn(cell.col, x > cw / 2 ? 1 : -1);
      }

      if( isvalid(this._refMain.current) ) {
        this._refMain.current.focus();
      }

      if( cell.col < 0 && cell.row < 0 ) { // Header click
        this.setState({ selectedRange:{col:0, row:0, col2:colCount, row2:rowCount} });
      } else if( shiftKey ) {
        if( preStatus === MA.SELCOL ) {
          this.setState({ selectedRange:{col:statusParam.colSel, row:0, col2:cell.col, row2:rowCount}, status:preStatus });
        } else if( preStatus === MA.SELROW ) {
          this.setState({ selectedRange:{col:0, row:statusParam.rowSel, col2:colCount, row2:cell.row}, status:preStatus });
        } else if( preStatus === MA.SELCELL ) {
          const { colSel, rowSel } = statusParam;
          this.setState({ selectedRange:{col:colSel, row:rowSel, col2:cell.col, row2:cell.row}, status:preStatus });
        }
      } else {
        let newStatus = status;
        if( cell.row < 0 ) { // Column Header click
          if( cell.colEdge ) {  // Column Sizing
            newStatus = MA.SIZING;
          } else { // Column Selecting
            cell.row = 0;
            cell.row2 = rowCount;
            newStatus = MA.SELCOL;
          }
        } else if( cell.col < 0 ) { // Row Header click --> Row Selecting
          cell.col = 0;
          cell.col2 = colCount;
          newStatus = MA.SELROW;
        } else {
          newStatus = MA.SELCELL;
        }

        const cTick = tickCount();

        if( newStatus === MA.SIZING ) {
          this.setState({
            status: newStatus,
            statusParam: {colSel: cell.col, x1: x, y1: y, size: this.getColumnWidth(cell.col) },
            clickTick: [clickTick[1], cTick]
          });
        } else {
          this.setState({
            selectedRange: cell,
            status: newStatus,
            statusParam: { colSel: cell.col, rowSel: cell.row },
            clickTick: [clickTick[1], cTick]
          });

          //
          this.relayEvent(GridEvent.CELL_SELECTED, { column:cell.col, row:cell.row, status:newStatus });
        }
      }
      break;

    case 'mouseup':
      // double-click cell edge
      if( status === MA.SIZING && preStatus === MA.SIZING && tickCount() - clickTick[0] < 400 ) {
        const cIdx = statusParam.colSel;
        const oldWidth = this.getColumnWidth(cIdx);
        this.setColumnWidth(cIdx, oldWidth > 28 ? 28 : 200);  // TODO 원래 컬럼
      }
      this.setState({ status:MA.NORMAL, preStatus:status });
      break;

    case 'mousemove':
      switch( status ) {
        case MA.SIZING:
        {
          const { colSel, x1, size } = statusParam;
          this.setColumnWidth(colSel, Math.max(ds.getRowHeight() + 2, size + x - x1));
        } break;
        case MA.SELCOL:
          this.setState({ selectedRange:{col:statusParam.colSel, row:0, col2:cell.col, row2:rowCount} });
          break;
        case MA.SELROW:
          this.setState({ selectedRange:{col:0, row:statusParam.rowSel, col2:colCount, row2:cell.row} });
          break;
        case MA.SELCELL:
        {
          const { colSel, rowSel } = statusParam;
          this.setState({ selectedRange:{col:colSel, row:rowSel, col2:cell.col, row2:cell.row} });
        } break;
        default:
          this.setState({ overCell: cell });
          break;
      }
      break;

    default:
      break;
    }
  }

  handleMouseEnter = () => {
    // document.body.style['pointer-events'] = 'none';

    if( isvalid(this._refMain.current) ) {
      this._refMain.current.focus();
    }
  }

  handleMouseLeave = () => {
    // document.body.style['pointer-events'] = 'auto';
  }

  // type: open(filter), close(filter), pinned, unpinned
  // param { colIdx, pos:{x,y}, ... }
  // open일 경우 param에 cbClose:func 추가됨.
  handleColumnHeaderEvent = (type, param) => {
    switch( type ) {
    case 'open':
      this.setState({ activeFilter: param.colIdx, filterPos: param.pos, cbFilterClose: param.cbClose });
      break;
    case 'close':
      this.setState({ activeFilter:-1, filterPos:null, cbFilterClose:null });
      break;

    case 'pinned':
      this.setState( DataGrid.recalculateDimension(this.props, this.state, null, null, null, param.colIdx + 1) );
      this._refMain.current.focus();
      break;

    case 'unpinned':
      this.setState( DataGrid.recalculateDimension(this.props, this.state, null, null, null, param.colIdx) );
      this._refMain.current.focus();
      break;

    default:
      break;
    }
  }

  handleCloseFilter = () => {
    const { cbFilterClose } = this.state;

    if( cbFilterClose ) {
      cbFilterClose();
    }

    this.setState({ activeFilter:-1, filterPos:null, cbFilterClose:null });
  }

  handleDoneFilter = (filter) => {
    if( isvalid(filter) ) {
      const { ds } = this.state;
      // Parent한테 알리는 건 DataSource에서 하자.
      ds.setColumnFilterData(this.state.activeFilter, filter);
    }

    this.handleCloseFilter();
  }

  relayEvent = (eventType, option) => {
    const { onEvent } = this.props;
    if( isundef(onEvent) ) {
      return;
    }

    switch( eventType ) {
      case GridEvent.CELL_SELECTED: {
        const { column, row, status } = option;
        if( status === MA.SELCOL ) {
          onEvent(GridEvent.COLUMN_SELECTED, { column });
        } else if( status === MA.SELROW ) {
          onEvent(GridEvent.ROW_SELECTED, { row });
        } else {
          onEvent(GridEvent.CELL_SELECTED, { column, row });
        }
      } break;

      default:
        break;
    }
  }

  doHoldEvent = (set, c, r) => {
    this._eventHold = set;

    if( !set ) {
      this._refMain.current.focus();
      return;
    }

    // Holding 되면서 지정된 컬럼(c)가 있는 경우 컬럼(c)가 보이도록 스크롤 조정해야 함
    if( isvalid(c) ) {
      const { columnWidth, scrollLeft, chWidth } = this.state;

      if( columnWidth[c] < scrollLeft ) {
        this.setScrollLeft(columnWidth[c]);
      } else if( columnWidth[c + 1] > scrollLeft + chWidth ) {
        this.setScrollLeft(columnWidth[c + 1] - chWidth + 1);
      }
    }
  }

  findAll = (keyword, caseSensitive, cpos) => {
    const { ds } = this.state;

    const result = [];
    let cidx = null, ridx = null;

    const cc = cpos.row * ds.getColumnCount() + cpos.col;

    for(let r = 0; r < ds.getRowCount(); ++r) {
      const rs = [];
      for(let c = 0; c < ds.getColumnCount(); ++c) {
        const cell = ds.getCellValue(c, r);
        if( cell === null ) {
          continue;
        }

        if( ('' + cell).indexOf(keyword) !== -1 ) {
          if( cidx === null && cc < r * ds.getColumnCount() + c ) {
            cidx = rs.length
            ridx = result.length;
          }
          rs.push(c);
        }
      }

      if( rs.length > 0 ) {
        result.push({ r, rs });
      }
    }

    if( result.length > 0 && cidx === null ) {
      cidx = 0;
      ridx = 0;
    }

    return { result, cidx, ridx, keyword, caseSensitive };
  }

  onFindEvent = (type, param, cb) => {
    switch( type ) {
      case 'find': {
        // param: keyword, caseSensitive
        // console.log('todo finding keyword', param);
        this._findRes = this.findAll(param.keyword, param.caseSensitive, this.state.selectedRange);
        this._findRes.foundIndex = this._findRes.result.map((o) => (o.r));

        const { result, cidx, ridx } = this._findRes;

        if( cb ) {
          cb(result, cidx, ridx);
        }

        if( result.length > 0 && isvalid(cidx) ) {
          // resut.nextPos 로 이동
          const fs = result[ridx];
          this.ensureVisibleCell(fs.rs[cidx], fs.r, true, true);
        }
      } break;

      case 'prev': {
        let { result, cidx, ridx } = this._findRes;

        if( isvalid(result) && result.length > 0 ) {
          const { row, col } = this.state.selectedRange;
          let comp = binarySearch(result, row, (x, y) => (x.r - y) );

          // 같은 레코드가 있는 경우는 컬럼을 비교해 봐야 함.
          if( comp.m !== -1 ) {
            ridx = comp.m;

            let comp2 = binarySearch(result[ridx].rs, col, (x, y) => (x - y));

            // 현재 선택된 컬럼과 같은 것이 있다면 앞에 것을 취하고,
            // 없다면 comp2.h를 취함
            cidx = comp2.m !== -1 ? comp2.m - 1 : comp2.h;

            // 0보다 작다면 레코드를 바꿔야 함.
            if( cidx < 0 ) {
              ridx = (ridx === 0 ? result.length : ridx) - 1;
              cidx = result[ridx].rs.length - 1;
            }
          } else {
            // 못 찾았을 경우에는 h에 이전에 해당하는 결과가 들어 가 있음
            ridx = comp.h < 0 ? result.length - 1 : comp.h;
            cidx = result[ridx].rs.length - 1;
          }

          const fs = result[ridx];
          this._findRes.cidx = cidx;
          this._findRes.ridx = ridx;

          this.ensureVisibleCell(fs.rs[cidx], fs.r, false, true);
        }
      } break;

      case 'next': {
        let { result, cidx, ridx } = this._findRes;

        if( isvalid(result) && result.length > 0 ) {
          const { row, col } = this.state.selectedRange;
          let comp = binarySearch(result, row, (x, y) => (x.r - y) );

          // 같은 레코드가 있는 경우는 컬럼을 비교해 봐야 함.
          if( comp.m !== -1 ) {
            ridx = comp.m;

            let comp2 = binarySearch(result[ridx].rs, col, (x, y) => (x - y));

            // 현재 선택된 컬럼과 같은 것이 있다면 뒤에 것을 취하고,
            // 없다면 comp2.l를 취함
            cidx = comp2.m !== -1 ? comp2.m + 1 : comp2.l;

            // 찾은 결과의 크기를 넘어 간다면 레코드를 바꿔야 함.
            if( cidx >= result[ridx].rs.length ) {
              ridx = (ridx >= result.length - 1 ? -1 : ridx) + 1;
              cidx = 0;
            }
          } else {
            // 못 찾았을 경우에는 h에 이전에 해당하는 결과가 들어 가 있음
            ridx = comp.l >= result.length ? 0 : comp.l;
            cidx = 0;
          }

          const fs = result[ridx];
          this._findRes.cidx = cidx;
          this._findRes.ridx = ridx;

          this.ensureVisibleCell(fs.rs[cidx], fs.r, false, true);
        }
      } break;

      case 'close':
        this.setState({ inFinding:false });
        this._refMain.current.focus();
        break;

      default:
        break;
    }
  }

  render () {
    const { fixableCount } = this.props;
    const {
      ds,
      showColumnNumber, beginRow, columnWidth, status, overCell,
      cw, ch, scrollLeft, activeFilter, filterPos, fixedColumn, inFinding,
      rowPerHeight, fixedColWidth, headerWidth, chHeight, rhWidth, rhHeight, chWidth, hScroll, vScroll
    } = this.state;

    const sbWidth = _scrollBarWith_;
    const width = cw, height = ch;

    const rowHeight = ds.getRowHeight(); // 한 행의 높이
    const rowCount = ds.getRowCount();
    const columnCount = ds.getColumnCount();

    const begin = beginRow;
    const end = Math.min(beginRow + rowPerHeight, rowCount);

    let dataTagList = [], rhTagList = [];

    for(let r = begin; r < end; ++r) {
      rhTagList.push(
        <RowHeader key={'rk-' + r} index={r}
          ds={ds}
          selected={this.isSelectedRow(r)}
          changeState={this.changeState}
          getColumnWidth={this.getColumnWidth}
          doHoldEvent={this.doHoldEvent}
          fixedColumn={fixedColumn}
          isSelected={this.isSelected}
          headerWidth={headerWidth}
        />
      );

      dataTagList.push(
        <DataRecord key={'dc-' + r} row={r}
          ds={ds}
          isSelected={this.isSelected}
          changeState={this.changeState}
          getColumnWidth={this.getColumnWidth}
          doHoldEvent={this.doHoldEvent}
          fixedColumn={fixedColumn}
        />
      );
    }

    const chTagList = [], fixeColTags = [];

    for(let c = 0; c < columnCount; ++c) {
      const colWidth = this.getColumnWidth(c);

      const columnTag = (
        <ColumnHeader
          key={'ck-' + c}
          index={c}
          title={ds.getColumnName(c)}
          width={colWidth}
          left={columnWidth[c] - (c < fixedColumn ? 0 : fixedColWidth)}
          selected={this.isSelectedColumn(c)}
          rowHeight={rowHeight}
          showNumber={showColumnNumber}
          hasFilter={ds.hasColumnFilterData && ds.hasColumnFilterData(c)}
          filtered={ds.isColumnFiltered && ds.isColumnFiltered(c)}
          onColumnEvent={this.handleColumnHeaderEvent}
          fixedType={c >= fixedColumn ? 0 : (c === fixedColumn - 1 ? 2 : 1)}
          fixable={c < fixableCount}
        />
      );

      if( c < fixedColumn ) {
        fixeColTags.push(columnTag);
      } else {
        chTagList.push(columnTag);
      }
    }

    const lineHeight = (rowHeight - 10) + 'px';
    const adjDataWidth = chWidth - (vScroll ? sbWidth : 0);
    const dcWidth = width - (vScroll ? sbWidth : 2);
    const dcHeight = height - (hScroll ? sbWidth : 0);

    return (
      <div
        ref={this._refMain}
        tabIndex="1"
        className="wrapGrid"
        onKeyDown={this.onKeyDown}
        onFocus={this.onFocus}
        onMouseEnter={this.handleMouseEnter}
        onMouseLeave={this.handleMouseLeave}
      >
        <div className="wrapContainer"
          style={{ height:dcHeight, flexBasis:dcHeight }}
          onMouseMove={this.onMouseEvent}
          onMouseDown={this.onMouseEvent}
          onMouseUp={this.onMouseEvent}
          onWheel={this.onDataAreaWheel}
        >
          <div className="wrapRow" style={{ flexBasis: rhWidth }}>
            <div style={{ width: rhWidth, height: chHeight, display:'flex' }}>
              <div className="headCorner"
                style={{ width: rhWidth - fixedColWidth, height: chHeight, lineHeight: lineHeight }}
              >
                &nbsp;
              </div>
              { fixeColTags.length > 0 &&
                <div
                  className={ cn({ 'headerCornerColumn': true, 'resizeCursor': (overCell.colEdge || status === MA.SIZING) }) }
                  style={{ width:fixedColWidth, height: chHeight, lineHeight: lineHeight }}
                >
                  { fixeColTags.map((tag) => tag) }
                </div>
              }
            </div>

            <div className="rowHeader" style={{ height: rhHeight, flexGlow: 1 }}>
              <div ref={this._elementRef['rowHeader']} className="rowCells">
                { rhTagList.map((tag) => tag) }
              </div>
            </div>
          </div>

          <div className="wrapColumn" style={{ width:adjDataWidth, height, flexBasis:adjDataWidth}}>
            <div ref={this._elementRef['columnArea']}
              className={ cn({ 'columnsDiv': true, 'resizeCursor': (overCell.colEdge || status === MA.SIZING) }) }
              style={{ height: chHeight }}
            >
              { chTagList.map((tag) => tag) }
            </div>
            <div ref={this._elementRef['dataContainer']}
              className="dataContainer"
              style={{ height: rhHeight }}
            >
              { dataTagList.map((tag) => tag) }
            </div>
          </div>
          { /* TODO 컬러맵 같은 특수한 콤포넌트가 포함되는 경우를 생각해 보자 */ }
          { vScroll &&
            <DataGridScrollBar
              barHeight={dcHeight - 2}
              barWidth={sbWidth}
              vertical={true}
              onPositionChanged={this.handleVScrollChanged}
              initialPosition={begin}
              visibleCount={rowPerHeight}
              total={rowCount}
              marker={this._findRes.foundIndex}
            />
          }
        </div>
        { hScroll &&
          <DataGridScrollBar
            barHeight={dcWidth}
            barWidth={sbWidth}
            vertical={false}
            onPositionChanged={this.handleHScrollChanged}
            initialPosition={scrollLeft}
            visibleCount={adjDataWidth}
            total={columnWidth[columnWidth.length - 1] - fixedColWidth}
          />
        }
        { activeFilter !== -1 &&
          <React.Fragment>
            <div className="overlayLayer" onClick={this.handleCloseFilter}>&nbsp;</div>
            <ColumnFilter
              top={filterPos.y}
              right={filterPos.x}
              data={ds.getColumnFilterData(activeFilter)}
              onDone={this.handleDoneFilter}
            />
          </React.Fragment>
        }
        { inFinding &&
          <FindDialog
            top={this._refMain.current.offsetTop + 2 + rowHeight}
            right={this._refMain.current.offsetLeft + this._refMain.current.offsetWidth - (vScroll ? sbWidth : 0) - 1}
            prevResult={this._findRes}
            onEvent={this.onFindEvent}
            doHoldEvent={this.doHoldEvent}
          />
        }
      </div>
    );
  }
}

export default DataGrid;
export { DataGrid, GridEvent };
