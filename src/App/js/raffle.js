let maskedPhones = new Array();
let realPhones = new Array();

const options = {
  method: "GET",
  headers: {
    'Access-Control-Allow-Origin':'*'
  }
};

// Get the post data
fetch('https://bgranados.ngrok.io/participants',options).
then(function (response) {
    return response.json();
})
.then(function (data) {
    var mainContainer = document.getElementById("headline2");
    console.log(data);
    mainContainer.innerHTML= data.Event + ' ' + data.Year;

    for (var i = 0; i < data.numbers.length; i++) {
        maskedPhones.push(data.numbers[i].anonymousValue);
    }

    for (var i = 0; i < data.numbers.length; i++) {
        realPhones.push(data.numbers[i].phoneNumber);
    }
})
.catch(function (err) {
    console.warn(err);
});



