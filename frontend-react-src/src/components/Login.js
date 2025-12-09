import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { loginUser } from '../apiService';

function Login({ setIsLoggedIn }) { // Props로 상태 변경 함수 수신
    const [credentials, setCredentials] = useState({ username: '', password: '' });
    const [error, setError] = useState(null);
    const navigate = useNavigate();

    const handleChange = (e) => {
        const { name, value } = e.target;
        setCredentials(prev => ({ ...prev, [name]: value }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            const response = await loginUser(credentials.username, credentials.password);
            if (response.status === 200) {
                alert('로그인 성공!');

                localStorage.setItem('isLoggedIn', 'true');

                setIsLoggedIn(true);
                navigate('/');
            }
        } catch (err) {
            console.error(err);
            setError('로그인 실패: 아이디 또는 비밀번호를 확인하세요.');
        }
    };

    return (
        <div className="container" style={{ maxWidth: '400px', marginTop: '50px' }}>
            <h2>로그인</h2>
            <form onSubmit={handleSubmit} className="post-form">
                <input
                    type="text"
                    name="username"
                    placeholder="아이디"
                    value={credentials.username}
                    onChange={handleChange}
                    required
                />
                <input
                    type="password"
                    name="password"
                    placeholder="비밀번호"
                    value={credentials.password}
                    onChange={handleChange}
                    required
                />
                {error && <div className="error-msg">{error}</div>}
                <button type="submit">로그인</button>
            </form>
        </div>
    );
}

export default Login;