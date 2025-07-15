// src/contexts/UserContext.js
import React, { createContext, useContext, useState, useEffect } from 'react';

const UserContext = createContext();

export function UserProvider({ children }) {
    const [user, setUser] = useState(null); // user: null이면 비로그인
    const [isLoading, setIsLoading] = useState(true); // 로딩 상태

    useEffect(() => {
        // 앱이 처음 로드될 때 서버에서 유저 정보 불러오기
        fetch(`http://localhost:80/api/current-user`, { credentials: 'include' })
            .then(res => res.json())
            .then(data => {
                if (data.success && data.user) {
                    //console.log(data.user);
                    setUser(data.user);
                } else {
                    setUser(null);
                }
                setIsLoading(false);
            })
            .catch(() => {
                setUser(null);
                setIsLoading(false);
            });
    }, []);

    return (
        <UserContext.Provider value={{ user, setUser, isLoading }}>
            {children}
        </UserContext.Provider>
    );
}

export function useUser() {
    return useContext(UserContext);
}