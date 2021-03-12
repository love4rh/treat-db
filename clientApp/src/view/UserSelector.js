import React, { Component } from 'react';
import PropTypes from 'prop-types';

import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form'

import './UserSelector.scss';



class UserSelector extends Component {
  static propTypes = {
    userID: PropTypes.string.isRequired,
    onChangeUser: PropTypes.func
  }

  constructor (props) {
    super(props);

    this.state = {
      userID: props.userID,
      authCode: ''
    };

    this.authInput = React.createRef();
  }

  handleChangeUser = (ev) => {
    this.setState({userID: ev.target.value});
  }

  handleChangeAuth = (ev) => {
    this.setState({authCode: ev.target.value});
  }

  onKeyDown = (type) => (ev) => {
    if( ev.keyCode === 13 ) {
      ev.preventDefault();

      if( type === 'auth') {
        this.handleGo();
      } else {
        this.authInput.current.focus();
      }
    }
  }

  handleGo = () => {
    const { userID, authCode } = this.state;
    this.props.onChangeUser(userID, authCode);
  }

  render() {
    const { userID, authCode } = this.state;

    return (
      <div className="fullBox">
        <Form className="formBox">
          <Form.Group controlId="formUserId">
            <Form.Label className="userBox">Identifier</Form.Label>
            <Form.Control
              type="input"
              placeholder="enter test id"
              defaultValue={userID}
              onChange={this.handleChangeUser}
              onKeyDown={this.onKeyDown('user')}
            />
          </Form.Group>
          <Form.Group controlId="formAuthCode">
            <Form.Label className="userBox">Passphrase</Form.Label>
            <Form.Control
              ref={this.authInput}
              type="password"
              placeholder="enter passphrase"
              defaultValue={authCode}
              onChange={this.handleChangeAuth}
              onKeyDown={this.onKeyDown('auth')}
            />
          </Form.Group>
          <Button variant="primary" onClick={this.handleGo}>{'Go'}</Button>
        </Form>
      </div>
    );
  }
}

export default UserSelector;
