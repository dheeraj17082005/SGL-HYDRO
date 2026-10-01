import React from 'react';

export function Plate({ plate = 'GJ00XX0000', size = 'normal' }) {
  const cleanPlate = (plate || 'GJ00XX0000').toUpperCase().replace(/[\s-]/g, '');

  return (
    <div className={`hsrp-plate ${size === 'large' ? 'large' : ''}`} aria-label={`Vehicle registration plate ${cleanPlate}`}>
      <div className="hsrp-plate-blue-strip">
        <span className="chakra">☸</span>
        <span>IND</span>
      </div>
      <span className="hsrp-plate-number">{cleanPlate}</span>
    </div>
  );
}
