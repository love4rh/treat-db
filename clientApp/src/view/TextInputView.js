import React, { Component } from 'react';
import PropTypes from 'prop-types';

import InputGroup from 'react-bootstrap/InputGroup'
import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';

import './TextInputView.scss';



class TextInputView extends Component {
  static propTypes = {
    title: PropTypes.string,
    tip: PropTypes.string,
    value: PropTypes.string,
    onChange: PropTypes.func
  }

  constructor (props) {
    super(props);

    this.state = {
      value: props.value
    };
  }

  handleChange = (ev) => {
    this.setState({ value: ev.target.value });
  }


  onKeyDown = (type) => (ev) => {
    if( ev.keyCode === 13 ) {
      ev.preventDefault();
      this.handleGo();
    }
  }

  handleClick = (ev) => {
    ev.preventDefault();
    ev.stopPropagation();
  }

  handleGo = () => {
    const { value } = this.state;
    this.props.onChange(value);
  }

  render() {
    const { title, tip, controlId } = this.props;
    const { value } = this.state;

    return (
      <div className="fullBox" onClick={this.handleClick}>
        <Form className="formBox">
          <Form.Group controlId={controlId}>
            <Form.Label className="userBox">{title}</Form.Label>
            <InputGroup className="mb-3">
              <Form.Control
                type="input"
                placeholder={tip}
                defaultValue={value}
                onChange={this.handleChange}
                onKeyDown={this.onKeyDown('user')}
                autoFocus
              />
              <InputGroup.Append>
                <Button variant="primary" onClick={this.handleGo}>{'Go'}</Button>
              </InputGroup.Append>
            </InputGroup>
          </Form.Group>
        </Form>
      </div>
    );
  }
}

export default TextInputView;
