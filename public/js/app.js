$(document).ready(function() {
  $("li.track a").click(function(event) {
    if (!!(document.createElement('audio').canPlayType)) {
      var container = document.getElementById("audio-container");
      var audio = document.createElement("audio");

      while (container.firstChild) container.removeChild(container.firstChild);

      audio.src = this.getAttribute("href");
      audio.autoplay = "autoplay";
      audio.controls = "controls";
      container.appendChild(audio);
      event.preventDefault();
    }
  })
})