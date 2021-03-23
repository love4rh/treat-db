import React from 'react';
import PropTypes from 'prop-types';



export const DividerDirection = {
  vertical: 'vertical',
  horizontal: 'horizontal'
};


class LayoutDivider extends React.Component {
	static propTypes = {
    direction: PropTypes.oneOf(Object.keys(DividerDirection)).isRequired,
    onLayoutChange: PropTypes.func.isRequired,
    size: PropTypes.number // default 7
  }

  constructor (props) {
    super(props);

    this.state = {
    	p1: 0,
      p2: 0,
      dragging: false
    };

    this._refWrapper = React.createRef();
  }

  handleMouseDown = (ev) => {
    const p = this.isVertical() ? ev.clientX : ev.clientY;

    // console.log('LayoutDivider', ev.clientX, ev.clientY, p);

    this._refWrapper.current.focus();

    this.setState({ p1: p, p2: p, dragging: true });

    document.body.style.cursor = this.isVertical() ? 'ew-resize' : 'ns-resize';

    ev.preventDefault();
    ev.stopPropagation();

    document.addEventListener('mousemove', this.handleMouseMove, {capture: true});
    document.addEventListener('mouseup', this.handleMouseUp, {capture: true});
  }

  handleMouseMove = (ev) => {
    this.setState({ p2: (this.isVertical() ? ev.clientX : ev.clientY) });
  }

  handleMouseUp = (ev) => {
    const p = this.isVertical() ? ev.clientX : ev.clientY;

    // console.log('LayoutDivider Up', ev.clientX, ev.clientY, p);

    document.body.style.cursor = 'auto';

    document.removeEventListener('mousemove', this.handleMouseMove, {capture: true});
    document.removeEventListener('mouseup', this.handleMouseUp, {capture: true});

    if( this.isVertical() ) {
      this.props.onLayoutChange(this.state.p1, p);
    } else {
      this.props.onLayoutChange(p, this.state.p1);
    }
    this.setState({ p1: 0, p2 : 0, dragging: false });
  }

  isVertical = () => {
    return this.props.direction === DividerDirection.vertical;
  }

  render () {
    const borderColor = '#2ea8bf';
    const size = this.props.size ? this.props.size : 7;

    let mainCss, dragCss;

    if( this.isVertical() ) {
      mainCss = {
        flexBasis:size,
        width:`${size}px`,
        height:'100%',
        borderLeft: `1px solid ${borderColor}`,
        cursor:'ew-resize'
      };

      dragCss = {
        zIndex:99999,
        position:'fixed',
        left:`${this.state.p2}px`,
        backgroundColor:'darkGray',
        width:`${size}px`,
        height:'100%'
      };
    } else {
      mainCss = {
        flexBasis:size,
        height:`${size}px`,
        width:'100%',
        borderTop: `1px solid ${borderColor}`,
        cursor:'ns-resize'
      };

      dragCss = {
        zIndex:99999,
        position:'fixed',
        top:`${this.state.p2}px`,
        backgroundColor:'darkGray',
        height:`${size}px`,
        width:'100%'
      };
    }

  	return (
      <div
        ref={this._refWrapper}
        style={mainCss}
        onMouseDown={this.handleMouseDown}
      >
        { this.state.dragging && (
          <div style={dragCss}>
          </div>
        )}
      </div>
    );
  }
}

export default LayoutDivider;
export { LayoutDivider };
