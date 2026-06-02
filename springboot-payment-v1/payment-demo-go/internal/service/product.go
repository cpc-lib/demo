package service

import "payment-demo-go/internal/model"

func (s *Service) ListProducts() ([]model.Product, error) {
	var list []model.Product
	err := s.DB.Find(&list).Error
	return list, err
}
