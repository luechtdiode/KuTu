import{c as e}from"./chunk-PL2JVG7G.js";import{b as a,g as r,h as s,l as n}from"./chunk-ACERUE7O.js";import"./chunk-2R6CW7ES.js";var i=":host{left:0;right:0;top:0;bottom:0;display:block;position:absolute;-webkit-transform:translateZ(0);transform:translateZ(0);contain:strict;cursor:pointer;opacity:0.01;-ms-touch-action:none;touch-action:none;z-index:2}:host(.backdrop-hide){background:transparent}:host(.backdrop-no-tappable){cursor:auto}:host{background-color:var(--ion-backdrop-color, #000)}",c=i,p=":host{left:0;right:0;top:0;bottom:0;display:block;position:absolute;-webkit-transform:translateZ(0);transform:translateZ(0);contain:strict;cursor:pointer;opacity:0.01;-ms-touch-action:none;touch-action:none;z-index:2}:host(.backdrop-hide){background:transparent}:host(.backdrop-no-tappable){cursor:auto}:host{background-color:var(--ion-backdrop-color, #000)}",d=p,l=(()=>{let t=class{constructor(o){a(this,o),this.ionBackdropTap=n(this,"ionBackdropTap",7),this.visible=!0,this.tappable=!0,this.stopPropagation=!0}onMouseDown(o){this.emitTap(o)}emitTap(o){this.stopPropagation&&(o.preventDefault(),o.stopPropagation()),this.tappable&&this.ionBackdropTap.emit()}render(){let o=e(this);return r(s,{key:"c803b4302c8e722064feb03dafe3cb6e190b4f2b",tabindex:"-1","aria-hidden":"true",class:{[o]:!0,"backdrop-hide":!this.visible,"backdrop-no-tappable":!this.tappable}})}};return t.style={ios:c,md:d},t})();export{l as ion_backdrop};
