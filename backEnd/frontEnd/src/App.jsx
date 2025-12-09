import { useEffect, useState } from "react";
import reactLogo from './assets/react.svg'
import viteLogo from '/vite.svg'

import './App.css'
import axios from "axios";

function App() {
    const [msg, setMsg] = useState("");

    useEffect(() => {
        axios
            .get("http://localhost:8080/api/hello")
            .then((res) => setMsg(res.data))
            .catch((err) => console.error(err));
    }, []);

    return (
        <div>
            <h1>React + Spring 연동 테스트</h1>
            <p>{msg}</p>
        </div>
    );
}

export default App;
