////////////////////////////////////////////////////////////////////////////////
// from tool.js
export const isundef = (o) => {
  return o === null || typeof(o) === 'undefined';
}


export const isvalid = (o) => {
  return !isundef(o);
}


export const isBetween = (val, f, t) => {
  return f <= val && val < t;
}


export const nvl = (val, def) => {
  return isundef(val) ? def : val;
}


// 출처: https://stove99.tistory.com/113 [스토브 훌로구]
export const numberWithCommas = (x) => {
  var reg = /(^[+-]?\d+)(\d{3})/;
  var n = (x + '');

  while( reg.test(n) )
    n = n.replace(reg, '$1,$2');

  return n;
}


export const calcDigits = (n) => {
  return Math.log(n) * Math.LOG10E + 1 | 0;
}


export const calcDigitsWithCommas = (x) => {
  const d = calcDigits(x)
  return d + Math.floor((d - 1) / 3);
}


export const istrue = (x) => {
  return isvalid(x) && x;
}


export const makeid = (digitNum) => {
  let text = '';
  const possible = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';

  for (let i = 0; i < digitNum; ++i) {
    text += possible.charAt(Math.floor(Math.random() * possible.length));
  }

  return text;
}


export const tickCount = () => {
  return new Date().getTime();
}


export const binarySearch = (sorted, target, compare) => {
  let l = 0, h = sorted.length - 1;

  // console.log('binarySearch', l, h, target);

  while( l <= h ) {
    const m = Math.floor((l + h) / 2);
    const r = compare(sorted[m], target);

    if( r === 0 ) {
      return { m, l, h };
    } else if( r > 0 ) {
      h = m - 1;
    } else {
      l = m + 1;
    }
  }

  return { m:-1, l, h };
}


export const makeRowFilter = (ds, cb) => {
  const sm = [];
  for(let c = 0; c < ds.getColumnCount(); ++c) {
    const fd = ds.getColumnFilterData(c);

    if( isundef(fd) ) {
      sm.push(null);
    } else {
      sm.push( fd.filter((d) => d.selected).reduce((m, d) => ({...m, [d.title]:true}), {}) );
    }
  }

  // console.log('makeRowFilter', sm);

  const ri = [];
  for(let r = 0; r < ds._getRowCount(true); ++r) {
    let selected = true;
    for(let c = 0; selected && c < ds.getColumnCount(); ++c) {
      selected = isundef(sm[c]) || isvalid(sm[c][ds._getRawCellValue(c, r)]);
    }

    if( selected ) {
      ri.push(r);
    }
  }

  // console.log('makeRowFilter result', ri);

  if( cb ) {
    cb({ rowFilter:ri });
  }
}


// 현재 Active 상태인 그리드
let _currentActiveGrid = null;

export const setCurrentActiveGrid = (grid) => {
  _currentActiveGrid = grid;
}


export const dismissActiveGrid = (grid) => {
  if( _currentActiveGrid === grid ) {
    _currentActiveGrid = null;
  }
}


export const getCurrentActiveGrid = () => {
  return _currentActiveGrid;
}


export const printDimension = (title, tag) => {
  const { offsetLeft, offsetTop, offsetWidth, offsetHeight } = tag;

  console.log(title, offsetLeft, offsetTop, offsetWidth, offsetHeight);
}