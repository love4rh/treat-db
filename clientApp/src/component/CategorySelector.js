import React, { Component } from 'react';
import PropTypes from 'prop-types';

import Form from 'react-bootstrap/Form'
import Button from 'react-bootstrap/Button'
import InputGroup from 'react-bootstrap/InputGroup'
import FormControl from 'react-bootstrap/FormControl'
import OverlayTrigger from 'react-bootstrap/OverlayTrigger';
import Tooltip from 'react-bootstrap/Tooltip';

// import { BsListCheck } from 'react-icons/bs';
import { RiCheckboxBlankLine, RiCheckboxLine, RiCheckboxFill } from "react-icons/ri";

import './CategorySelector.scss';



class CategorySelector extends Component {
	static propTypes = {
    index: PropTypes.number.isRequired,
		onChanged: PropTypes.func.isRequired,
    optionList: PropTypes.array.isRequired,
    selectList: PropTypes.array.isRequired,
    title: PropTypes.string.isRequired,
    itemColors: PropTypes.array
  }

  constructor (props) {
    super(props);

    const { optionList, selectList } = this.props;

    this.state = {
      selected: optionList.filter((v, idx) => selectList[idx]).join(', '),
      selectList: JSON.parse(JSON.stringify(selectList)),
      listOpen: false,
      sKeyword: ''
    };

    this._refMain = React.createRef();
  }

  componentDidMount () {
    window.addEventListener('resize', this.onResize);

    const { offsetTop, offsetLeft, offsetWidth } = this._refMain.current;
    this.setState({ cTop: offsetTop, cLeft: offsetLeft, cWidth: offsetWidth });
  }

  componentWillUnmount () {
    window.removeEventListener('resize', this.onResize);
  }

  onResize = () => {
    const { offsetTop, offsetLeft, offsetWidth } = this._refMain.current;
    this.setState({ cTop: offsetTop, cLeft: offsetLeft, cWidth: offsetWidth });
  }

  handleClick = () => {
    // console.log('handleClick', this.props.title);
    const { listOpen } = this.state;

    // 편집 목록이 열린 상태
    if( listOpen ) {
      this.handleLostFocus();      
    } else {
      this.setState({ listOpen: true, sKeyword: '' });
    }
  }

  changeItemChecked = (idx, checked) => {
    const { optionList } = this.props;
    const { selectList } = this.state;

    selectList[idx] = checked;
    this.setState({
      selected: optionList.filter((v, idx) => selectList[idx]).join(', '),
      selectList: selectList,
    });    
  }

  handleSelectClick = (idx) => (ev) => {
    ev.preventDefault();
    this.changeItemChecked(idx, !this.state.selectList[idx]);
  }

  handleChangeSelect = (idx) => (ev) => {
    console.log('handleChangeSelect', idx, ev.target.checked);
    this.changeItemChecked(idx, ev.target.checked);
  }

  handleLostFocus = () => {
    this.setState({ listOpen: false });
    this.props.onChanged(this.state.selectList);
  }

  handleChangeKeyword = (ev) => {
    const value = ev.target.value.trim();

    this.setState({ sKeyword: value });
  }

  handleSelectAll = (value) => () => {
    const { optionList } = this.props;
    const { sKeyword } = this.state;

    const selectList = optionList.map((n, idx) => {
      return ( sKeyword === '' || n.indexOf(sKeyword) !== -1 ) ? value : !value;
    });

    this.setState({
      selected: optionList.filter((v, idx) => selectList[idx]).join(', '),
      selectList: selectList
    });
  }

  render () {
  	const { index, title, optionList, itemColors } = this.props;
    const { selected, selectList, listOpen, sKeyword, cTop, cLeft, cWidth } = this.state;

    // onBlur={this.handleLostFocus}

  	return (
  		<div ref={this._refMain} className="catMainBox">
        <Form>
          <Form.Group controlId={`formCategory${index}`}>
            <Form.Label className="catTitle">{title}</Form.Label>
            <Form.Control
              className="catSelected"
              type="input"
              readOnly
              value={selected}
              onClick={this.handleClick}
            />
          </Form.Group>
        </Form>
        { listOpen &&
          <div>
            <div
              className="catPopupBox"
              style={{ top:(cTop + 78) + 'px', left:(cLeft + 7) + 'px', width:(cWidth - 14) + 'px' }}
            >
              <InputGroup className="catPopupMenu">
                <InputGroup.Prepend>
                  <OverlayTrigger
                    key={`unselbtn-overtip-${index}`}
                    placement={'bottom'}
                    overlay={ <Tooltip id={`unselbtn-tip-${index}`}>{'모두 선택 해제'}</Tooltip> }
                  >
                    <Button variant="warning" onClick={this.handleSelectAll(false)}><RiCheckboxBlankLine size="20" color="#000000" /></Button>
                  </OverlayTrigger>
                  <OverlayTrigger
                    key={`selbtn-overtip-${index}`}
                    placement={'bottom'}
                    overlay={ <Tooltip id={`selbtn-tip-${index}`}>{'모두 선택'}</Tooltip> }
                  >
                    <Button variant="primary" onClick={this.handleSelectAll(true)}><RiCheckboxLine size="20" color="#ffffff" /></Button>
                  </OverlayTrigger>
                </InputGroup.Prepend>
                <FormControl
                  type="input"
                  placeholder="Search..."
                  value={sKeyword}
                  onChange={this.handleChangeKeyword}
                />
              </InputGroup>

              <div className="catPopupListBox">
                <Form className="catPopupList">
                  <Form.Group>
                    { optionList.map((n, idx) => {
                        if( sKeyword !== '' && n.indexOf(sKeyword) === -1 ) {
                          return null;
                        }
                        return (
                          <div key={`ci-${index}-${idx}`} className="catPopupItem" onClick={this.handleSelectClick(idx)}>
                            { selectList[idx] ? <RiCheckboxFill size="18" color="#007bff" /> : <RiCheckboxBlankLine size="18" color="#007bff" /> }
                            { itemColors ? <span style={{ color:'#' + itemColors[idx] }}>{n}</span> : <span>{n}</span> }
                          </div>
                        );
                      }
                    )}
                  </Form.Group>
                </Form>
              </div>
            </div>
            <div className="catOverlay" onClick={this.handleLostFocus}>&nbsp;</div>
          </div>
        }
  		</div>
  	);
  }


}

export default CategorySelector;
