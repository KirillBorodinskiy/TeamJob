<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Calendar</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
<div id="container">
    <div id="calendar">
        <div>Monday</div>
        <div>Tuesday</div>
        <div>Wednesday</div>
        <div>Thursday</div>
        <div>Friday</div>
        <div>Saturday</div>
        <div>Sunday</div>
        @for (int i = 0; i < 17; i++) {
            <div></div>
        }
        @endfor
    </div>
</div>
</body>
</html>